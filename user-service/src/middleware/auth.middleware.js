const { verifyAccessToken } = require('../utils/jwt.utils');
const Unauthorized = require('../errors/unauthorized.error');
const { StatusCodes } = require('http-status-codes');

/**
 * Middleware to authenticate JWT token
 */
function authenticate(req, res, next) {
    try {

        // Get token from header
        const authHeader = req.headers.authorization;
        

        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            throw new Unauthorized('No token provided');
        }

        // Extract token
        const token = authHeader.substring(7); // Remove 'Bearer ' prefix

        // Verify token
        const decoded = verifyAccessToken(token);

        // Attach user to request
        req.user = decoded;

        next();

    } catch (error) {
        
        if (error instanceof Unauthorized) {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: error.message,
                data: {}
            });
        }

        // JWT errors
        if (error.name === 'JsonWebTokenError') {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: 'Invalid token',
                data: {}
            });
        }

        if (error.name === 'TokenExpiredError') {
            return res.status(StatusCodes.UNAUTHORIZED).json({
                success: false,
                message: 'Token expired',
                data: {}
            });
        }

        return res.status(StatusCodes.UNAUTHORIZED).json({
            success: false,
            message: 'Authentication failed',
            data: {}
        });
    }
}

module.exports = authenticate;
