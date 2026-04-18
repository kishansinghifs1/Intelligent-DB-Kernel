const { UserService } = require('../services');
const { StatusCodes } = require('http-status-codes');
const logger = require('../config/logger.config');

class UserController {
    constructor() {
        this.userService = new UserService();
    }

    /**
     * Register a new user
     * POST /api/v1/users/register
     */
    async register(req, res, next) {
        try {
            const { username, email, password, firstName, lastName } = req.body;

            if (!username || !email || !password) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Username, email, and password are required',
                    data: {}
                });
            }

            const user = await this.userService.registerUser({
                username,
                email,
                password,
                firstName,
                lastName
            });

            return res.status(StatusCodes.CREATED).json({
                success: true,
                message: 'User registered successfully',
                data: user
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Login user
     * POST /api/v1/users/login
     */
    async login(req, res, next) {
        try {
            const { email, password } = req.body;

            if (!email || !password) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Email and password are required',
                    data: {}
                });
            }

            const result = await this.userService.loginUser(email, password);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: 'Login successful',
                data: result
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Refresh access token
     * POST /api/v1/users/refresh
     */
    async refresh(req, res, next) {
        try {
            const { refreshToken } = req.body;

            if (!refreshToken) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Refresh token is required',
                    data: {}
                });
            }

            const result = await this.userService.refreshAccessToken(refreshToken);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: 'Token refreshed successfully',
                data: result
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Get user profile
     * GET /api/v1/users/profile
     */
    async getProfile(req, res, next) {
        try {
            const userId = req.user.id;
            const user = await this.userService.getUserProfile(userId);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: 'User profile retrieved successfully',
                data: user
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Update user profile
     * PUT /api/v1/users/profile
     */
    async updateProfile(req, res, next) {
        try {
            const userId = req.user.id;
            const { firstName, lastName } = req.body;

            const updatedUser = await this.userService.updateUserProfile(userId, {
                firstName,
                lastName
            });

            return res.status(StatusCodes.OK).json({
                success: true,
                message: 'Profile updated successfully',
                data: updatedUser
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Change password
     * PUT /api/v1/users/password
     */
    async changePassword(req, res, next) {
        try {
            const userId = req.user.id;
            const { oldPassword, newPassword } = req.body;

            if (!oldPassword || !newPassword) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Old password and new password are required',
                    data: {}
                });
            }

            const result = await this.userService.changePassword(userId, oldPassword, newPassword);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: result.message,
                data: {}
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Upload Avatar
     * POST /api/v1/users/avatar
     */
    async uploadAvatar(req, res, next) {
        try {
            const userId = req.user.id;
            
            if (!req.file) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'No image file provided',
                    data: {}
                });
            }

            const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];
            if (!allowedMimeTypes.includes(req.file.mimetype)) {
                return res.status(StatusCodes.BAD_REQUEST).json({
                    success: false,
                    message: 'Invalid file type. Only JPG, PNG, and WebP are allowed.',
                    data: {}
                });
            }

            const updatedUser = await this.userService.uploadAvatar(userId, req.file);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: 'Avatar updated successfully',
                data: updatedUser
            });
        } catch (error) {
            next(error);
        }
    }

    /**
     * Logout user
     * POST /api/v1/users/logout
     */
    async logout(req, res, next) {
        try {
            const userId = req.user.id;
            const result = await this.userService.logoutUser(userId);

            return res.status(StatusCodes.OK).json({
                success: true,
                message: result.message,
                data: {}
            });
        } catch (error) {
            next(error);
        }
    }
}

module.exports = UserController;
