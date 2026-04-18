const { UserRepository } = require('../repositories');

const { comparePassword, validatePasswordStrength } = require('../utils/password.utils');
const { generateAccessToken, generateRefreshToken, verifyRefreshToken } = require('../utils/jwt.utils');

const BadRequest = require('../errors/badrequest.error');
const Unauthorized = require('../errors/unauthorized.error');

const logger = require('../config/logger.config');

const { PutObjectCommand } = require('@aws-sdk/client-s3');
const s3Client = require('../config/aws.config');

const redisClient = require('../config/redis.config');

const { USER_PROFILE_CACHE_TTL_SECONDS } = require('../config/server.config');

const crypto = require('crypto');

const fs = require('fs');

function hashToken(token) {
    return crypto.createHash('sha256').update(token).digest('hex');
}

class UserService {
    constructor() {
        this.userRepository = new UserRepository();
    }

    getUserProfileCacheKey(userId) {
        return `cache:user-profile:${userId}`;
    }

    async getCachedUserProfile(userId) {
        const cacheKey = this.getUserProfileCacheKey(userId);

        try {
            return await redisClient.getJson(cacheKey);
        } catch (error) {
            logger.warn(`UserService cache read failed for user ${userId}: ${error.message}`);
            return null;
        }
    }

    async setCachedUserProfile(userId, userProfile) {
        const cacheKey = this.getUserProfileCacheKey(userId);

        try {
            await redisClient.setJson(cacheKey, userProfile, USER_PROFILE_CACHE_TTL_SECONDS);
        } catch (error) {
            logger.warn(`UserService cache write failed for user ${userId}: ${error.message}`);
        }
    }

    async invalidateCachedUserProfile(userId) {
        const cacheKey = this.getUserProfileCacheKey(userId);

        try {
            await redisClient.deleteKey(cacheKey);
        } catch (error) {
            logger.warn(`UserService cache delete failed for user ${userId}: ${error.message}`);
        }
    }

    /**
     * Register a new user
     * @param {Object} userData - User registration data
     * @returns {Promise<Object>} - User object without password
     */
    async registerUser(userData) {
        try {
            // Validate password strength
            const passwordValidation = validatePasswordStrength(userData.password);
            if (!passwordValidation.isValid) {
                throw new BadRequest('password', passwordValidation.errors.join(', '));
            }

            // Check if email already exists
            const existingUserByEmail = await this.userRepository.getUserByEmail(userData.email);
            if (existingUserByEmail) {
                throw new BadRequest('email', 'Email already registered');
            }

            // Check if username already exists
            const existingUserByUsername = await this.userRepository.getUserByUsername(userData.username);
            if (existingUserByUsername) {
                throw new BadRequest('username', 'Username already taken');
            }

            // Create user
            const user = await this.userRepository.createUser(userData);

            logger.info(`New user registered: ${user.email}`);

            // Return user without password
            return user.toJSON();
        } catch (error) {
            logger.error('UserService.registerUser:', error);
            throw error;
        }
    }

    /**
     * Login user
     * @param {string} email - User email
     * @param {string} password - User password
     * @returns {Promise<Object>} - Access token, refresh token, and user data
     */
    async loginUser(email, password) {
        try {
            // Find user by email
            const user = await this.userRepository.getUserByEmail(email);
            if (!user) {
                throw new Unauthorized('Invalid email or password');
            }

            // Check if user is active
            if (!user.isActive) {
                throw new Unauthorized('Account is deactivated');
            }

            // Verify password
            const isPasswordValid = await comparePassword(password, user.password);
            if (!isPasswordValid) {
                throw new Unauthorized('Invalid email or password');
            }

            // Generate tokens
            const payload = {
                id: user._id,
                email: user.email,
                username: user.username,
                role: user.role
            };

            const accessToken = generateAccessToken(payload);
            const refreshToken = generateRefreshToken(payload);
            const decodedRefresh = verifyRefreshToken(refreshToken);

            await this.userRepository.setRefreshTokenSession(
                user._id,
                hashToken(refreshToken),
                new Date(decodedRefresh.exp * 1000)
            );
            await this.invalidateCachedUserProfile(user._id);

            logger.info(`User logged in: ${user.email}`);

            return {
                accessToken,
                refreshToken,
                user: user.toJSON()
            };
        } catch (error) {
            logger.error('UserService.loginUser:', error);
            throw error;
        }
    }

    /**
     * Refresh access token
     * @param {string} refreshToken - Refresh token
     * @returns {Promise<Object>} - New access token
     */
    async refreshAccessToken(refreshToken) {
        try {
            // Verify refresh token
            const decoded = verifyRefreshToken(refreshToken);

            // Get user from database
            const user = await this.userRepository.getUserById(decoded.id);

            if (!user.isActive) {
                throw new Unauthorized('Account is deactivated');
            }

            const incomingRefreshTokenHash = hashToken(refreshToken);
            if (!user.refreshTokenHash || user.refreshTokenHash !== incomingRefreshTokenHash) {
                throw new Unauthorized('Invalid refresh session');
            }

            if (!user.refreshTokenExpiresAt || user.refreshTokenExpiresAt.getTime() < Date.now()) {
                throw new Unauthorized('Refresh token expired');
            }

            // Generate new access token
            const payload = {
                id: user._id,
                email: user.email,
                username: user.username,
                role: user.role
            };

            const newAccessToken = generateAccessToken(payload);
            const newRefreshToken = generateRefreshToken(payload);
            const decodedNewRefresh = verifyRefreshToken(newRefreshToken);

            await this.userRepository.setRefreshTokenSession(
                user._id,
                hashToken(newRefreshToken),
                new Date(decodedNewRefresh.exp * 1000)
            );
            await this.invalidateCachedUserProfile(user._id);

            return {
                accessToken: newAccessToken,
                refreshToken: newRefreshToken
            };
        } catch (error) {
            logger.error('UserService.refreshAccessToken:', error);
            throw error;
        }
    }

    /**
     * Get user profile
     * @param {string} userId - User ID
     * @returns {Promise<Object>} - User profile
     */
    async getUserProfile(userId) {
        try {
            const cachedUserProfile = await this.getCachedUserProfile(userId);
            if (cachedUserProfile) {
                return cachedUserProfile;
            }

            const user = await this.userRepository.getUserById(userId);
            const userProfile = user.toJSON();

            await this.setCachedUserProfile(userId, userProfile);
            return userProfile;
        } catch (error) {
            logger.error('UserService.getUserProfile:', error);
            throw error;
        }
    }

    /**
     * Update user profile
     * @param {string} userId - User ID
     * @param {Object} updateData - Data to update (firstName, lastName)
     * @returns {Promise<Object>} - Updated user profile
     */
    async updateUserProfile(userId, updateData) {
        try {
            // Only allow updating specific fields
            const allowedUpdates = ['firstName', 'lastName'];
            const updates = {};

            for (const key of allowedUpdates) {
                if (updateData[key] !== undefined) {
                    updates[key] = updateData[key];
                }
            }

            if (Object.keys(updates).length === 0) {
                throw new BadRequest('updateData', 'No valid fields to update');
            }

            const updatedUser = await this.userRepository.updateUser(userId, updates);
            const updatedUserJson = updatedUser.toJSON();
            await this.setCachedUserProfile(userId, updatedUserJson);

            logger.info(`User profile updated: ${updatedUser.email}`);

            return updatedUserJson;
        } catch (error) {
            logger.error('UserService.updateUserProfile:', error);
            throw error;
        }
    }

    /**
     * Change user password
     * @param {string} userId - User ID
     * @param {string} oldPassword - Current password
     * @param {string} newPassword - New password
     * @returns {Promise<Object>} - Success message
     */
    async changePassword(userId, oldPassword, newPassword) {
        try {
            // Get user
            const user = await this.userRepository.getUserById(userId);

            // Verify old password
            const isPasswordValid = await comparePassword(oldPassword, user.password);
            if (!isPasswordValid) {
                throw new Unauthorized('Current password is incorrect');
            }

            // Validate new password strength
            const passwordValidation = validatePasswordStrength(newPassword);
            if (!passwordValidation.isValid) {
                throw new BadRequest('newPassword', passwordValidation.errors.join(', '));
            }

            // Update password with plain value; model pre-save hook hashes it once.
            user.password = newPassword;
            await user.save();
            await this.userRepository.clearRefreshTokenSession(userId);
            await this.invalidateCachedUserProfile(userId);

            logger.info(`Password changed for user: ${user.email}`);

            return {
                message: 'Password changed successfully'
            };
        } catch (error) {
            logger.error('UserService.changePassword:', error);
            throw error;
        }
    }

    /**
     * Upload user avatar to S3
     * @param {string} userId - User ID
     * @param {Object} file - Multer file object
     * @returns {Promise<Object>} - Updated user profile
     */
    async uploadAvatar(userId, file) {
        let uploadedFilePath;
        try {
            const user = await this.userRepository.getUserById(userId);

            uploadedFilePath = file.path;
            const mimetype = file.mimetype;
            const extension = mimetype.split('/')[1] || 'png';
            const fileName = `avatars/${userId}-${Date.now()}.${extension}`;
            const bucketName = process.env.S3_BUCKET_NAME;

            if (!bucketName) {
                throw new Error('S3_BUCKET_NAME is not configured');
            }

            const command = new PutObjectCommand({
                Bucket: bucketName,
                Key: fileName,
                Body: fs.createReadStream(file.path),
                ContentType: mimetype,
                ContentLength: file.size
            });

            await s3Client.send(command);

            const avatarUrl = `https://${bucketName}.s3.${process.env.AWS_REGION}.amazonaws.com/${fileName}`;

            user.avatarUrl = avatarUrl;
            await user.save();
            const updatedUserProfile = user.toJSON();
            await this.setCachedUserProfile(userId, updatedUserProfile);

            logger.info(`Avatar updated for user: ${user.email}`);

            return updatedUserProfile;
        } catch (error) {
            logger.error('UserService.uploadAvatar:', error);
            throw error;
        } finally {
            if (uploadedFilePath) {
                fs.promises.unlink(uploadedFilePath).catch(() => {});
            }
        }
    }

    /**
     * Logout user by revoking refresh session
     * @param {string} userId - User ID
     * @returns {Promise<Object>} - Success message
     */
    async logoutUser(userId) {
        try {
            await this.userRepository.clearRefreshTokenSession(userId);
            await this.invalidateCachedUserProfile(userId);
            return { message: 'Logged out successfully' };
        } catch (error) {
            logger.error('UserService.logoutUser:', error);
            throw error;
        }
    }
}

module.exports = UserService;
