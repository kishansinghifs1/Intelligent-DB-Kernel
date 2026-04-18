const express = require('express');
const { UserController } = require('../../controllers');
const authenticate = require('../../middleware/auth.middleware');
const multer = require('multer');
const os = require('os');
const path = require('path');
const fs = require('fs');
const { createRateLimiter } = require('../../middleware/rateLimit.middleware');
const {
    AUTH_RATE_LIMIT_WINDOW_MS,
    AUTH_RATE_LIMIT_MAX,
    USER_API_RATE_LIMIT_WINDOW_MS,
    USER_API_RATE_LIMIT_MAX
} = require('../../config/server.config');

const uploadDir = path.join(os.tmpdir(), 'user-service-uploads');
fs.mkdirSync(uploadDir, { recursive: true });

const authRateLimiter = createRateLimiter({
    windowMs: AUTH_RATE_LIMIT_WINDOW_MS,
    max: AUTH_RATE_LIMIT_MAX,
    message: 'Too many authentication requests. Please try again later.',
    keyPrefix: 'auth'
});

const userApiRateLimiter = createRateLimiter({
    windowMs: USER_API_RATE_LIMIT_WINDOW_MS,
    max: USER_API_RATE_LIMIT_MAX,
    message: 'Too many requests for user APIs. Please slow down and try again.',
    keyPrefix: 'users'
});

const upload = multer({
    storage: multer.diskStorage({
        destination: (req, file, cb) => cb(null, uploadDir),
        filename: (req, file, cb) => {
            const extension = path.extname(file.originalname || '').toLowerCase();
            cb(null, `${Date.now()}-${Math.random().toString(36).slice(2)}${extension}`);
        }
    }),
    limits: {
        fileSize: 1024 * 1024 // 1MB limit (per user request)
    }
});

const userRouter = express.Router();
const userController = new UserController();

userRouter.use(userApiRateLimiter);

// Public routes
userRouter.post('/register', authRateLimiter, (req, res, next) => userController.register(req, res, next));
userRouter.post('/login', authRateLimiter, (req, res, next) => userController.login(req, res, next));
userRouter.post('/refresh', authRateLimiter, (req, res, next) => userController.refresh(req, res, next));

// Protected routes
userRouter.get('/profile', authenticate, (req, res, next) => userController.getProfile(req, res, next));
userRouter.put('/profile', authenticate, (req, res, next) => userController.updateProfile(req, res, next));
userRouter.put('/password', authenticate, (req, res, next) => userController.changePassword(req, res, next));
userRouter.post('/avatar', authenticate, upload.single('avatar'), (req, res, next) => userController.uploadAvatar(req, res, next));
userRouter.post('/logout', authenticate, (req, res, next) => userController.logout(req, res, next));

module.exports = userRouter;
