const logger = require('../config/logger.config');

const NotFound = require('../errors/notfound.error');

const { User } = require('../models');

class UserRepository {

    /**
     * Create a new user
     * @param {Object} userData - User data
     * @returns {Promise<Object>} - Created user
     */

    async createUser(userData) {
        try {
            const user = await User.create({
                username: userData.username,
                email: userData.email,
                password: userData.password,
                firstName: userData.firstName,
                lastName: userData.lastName,
                role: userData.role || 'user'
            });
            return user;
        } catch (error) {
            logger.error('UserRepository.createUser:', error);
             
        }
    }

    /**
     * Get user by ID
     * @param {string} id - User ID
     * @returns {Promise<Object>} - User object
     */
    async getUserById(id) {
        try {
            const user = await User.findById(id);
            if (!user) {
                throw new NotFound('User', id);
            }
            return user;
        } catch (error) {
            logger.error('UserRepository.getUserById:', error);
            throw error;
        }
    }

    /**
     * Get user by email
     * @param {string} email - User email
     * @returns {Promise<Object|null>} - User object or null
     */
    async getUserByEmail(email) {
        try {
            const user = await User.findOne({ email: email.toLowerCase() });
            return user;
        } catch (error) {
            logger.error('UserRepository.getUserByEmail:', error);
            throw error;
        }
    }

    /**
     * Get user by username
     * @param {string} username - Username
     * @returns {Promise<Object|null>} - User object or null
     */
    async getUserByUsername(username) {
        try {
            const user = await User.findOne({ username });
            return user;
        } catch (error) {
            logger.error('UserRepository.getUserByUsername:', error);
            throw error;
        }
    }

    /**
     * Update user
     * @param {string} id - User ID
     * @param {Object} updateData - Data to update
     * @returns {Promise<Object>} - Updated user
     */
    async updateUser(id, updateData) {
        try {
            const updatedUser = await User.findByIdAndUpdate(
                id,
                updateData,
                { new: true, runValidators: true }
            );
            if (!updatedUser) {
                throw new NotFound('User', id);
            }
            return updatedUser;
        } catch (error) {
            logger.error('UserRepository.updateUser:', error);
            throw error;
        }
    }

    /**
     * Delete user (soft delete by setting isActive to false)
     * @param {string} id - User ID
     * @returns {Promise<Object>} - Deleted user
     */
    async deleteUser(id) {
        try {
            const deletedUser = await User.findByIdAndUpdate(
                id,
                { isActive: false },
                { new: true }
            );
            if (!deletedUser) {
                throw new NotFound('User', id);
            }
            return deletedUser;
        } catch (error) {
            logger.error('UserRepository.deleteUser:', error);
            throw error;
        }
    }

    /**
     * Get all users
     * @returns {Promise<Array>} - Array of users
     */
    async getAllUsers() {
        try {
            const users = await User.find({ isActive: true });
            return users;
        } catch (error) {
            logger.error('UserRepository.getAllUsers:', error);
            throw error;
        }
    }

    /**
     * Store hashed refresh token session for a user
     * @param {string} id - User ID
     * @param {string} refreshTokenHash - SHA-256 hash of refresh token
     * @param {Date} refreshTokenExpiresAt - Refresh token expiry time
     * @returns {Promise<Object>} - Updated user
     */
    async setRefreshTokenSession(id, refreshTokenHash, refreshTokenExpiresAt) {
        try {
            const updatedUser = await User.findByIdAndUpdate(
                id,
                { refreshTokenHash, refreshTokenExpiresAt },
                { new: true }
            );
            if (!updatedUser) {
                throw new NotFound('User', id);
            }
            return updatedUser;
        } catch (error) {
            logger.error('UserRepository.setRefreshTokenSession:', error);
            throw error;
        }
    }

    /**
     * Revoke refresh token session for a user
     * @param {string} id - User ID
     * @returns {Promise<Object>} - Updated user
     */
    async clearRefreshTokenSession(id) {
        try {
            const updatedUser = await User.findByIdAndUpdate(
                id,
                { refreshTokenHash: null, refreshTokenExpiresAt: null },
                { new: true }
            );
            if (!updatedUser) {
                throw new NotFound('User', id);
            }
            return updatedUser;
        } catch (error) {
            logger.error('UserRepository.clearRefreshTokenSession:', error);
            throw error;
        }
    }
}

module.exports = UserRepository;
