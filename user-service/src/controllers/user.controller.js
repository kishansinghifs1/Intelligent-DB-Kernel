const { UserService } = require('../services');

const BadRequest = require('../errors/badrequest.error');
const BaseError = require('../errors/base.error');

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

            const { username, email, password, firstName, lastName } = req.body;

            if (!username || !email || !password) {
                throw new BadRequest('Username, email, and password are required');
            }

            const user = await this.userService.registerUser({
                username,
                email,
                password,
                firstName,
                lastName
            });
    }

    /**
     * Login user
     * POST /api/v1/users/login
     */
    async login(req, res, next) {
        
            const { email, password } = req.body;

            if (!email || !password) {
                throw new BadRequest('Email and password are required');
            }

            const result = await this.userService.loginUser(email, password);

    }

    /**
     * Refresh access token
     * POST /api/v1/users/refresh
     */
    async refresh(req, res, next) {

            const { refreshToken } = req.body;

            if (!refreshToken) {
                throw new BadRequest('Refresh token is required');
            }

            const result = await this.userService.refreshAccessToken(refreshToken);
    }

    /**
     * Get user profile
     * GET /api/v1/users/profile
     */
    async getProfile(req, res, next) {

            const userId = req.user.id;
            const user = await this.userService.getUserProfile(userId);

    }

    /**
     * Update user profile
     * PUT /api/v1/users/profile
     */
    async updateProfile(req, res, next) {
            const userId = req.user.id;
            const { firstName, lastName } = req.body;

            const updatedUser = await this.userService.updateUserProfile(userId, {
                firstName,
                lastName
            });
    }

    /**
     * Change password
     * PUT /api/v1/users/password
     */
    async changePassword(req, res, next) {

            const userId = req.user.id;
            const { oldPassword, newPassword } = req.body;

            if (!oldPassword || !newPassword) {
                throw new BadRequest('Old password and new password are required');
            }

            const result = await this.userService.changePassword(userId, oldPassword, newPassword);
    }

    /**
     * Upload Avatar
     * POST /api/v1/users/avatar
     */
    async uploadAvatar(req, res, next) {
       
            const userId = req.user.id;
            
            if (!req.file) {
                throw new BadRequest('No image file provided');
            }

            const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp'];
            if (!allowedMimeTypes.includes(req.file.mimetype)) {
                throw new BadRequest('Invalid file type. Only JPEG, PNG, and WEBP are allowed.');
            }

            const updatedUser = await this.userService.uploadAvatar(userId, req.file);

    }

    /**
     * Logout user
     * POST /api/v1/users/logout
     */
    async logout(req, res, next) {

            const userId = req.user.id;
            const result = await this.userService.logoutUser(userId);
    }
}

module.exports = UserController;
