const BaseError = require('../errors/base.error');
const RequestValidationError = require('../errors/validation.error');
const ConflictError = require('../errors/conflict.error');
const Unauthorized = require('../errors/unauthorized.error');
const InternalServerError = require('../errors/internalserver.error');
const logger = require('../config/logger.config');
const { NODE_ENV } = require('../config/server.config');

function errorHandler(err, req, res, next) {
    logger.error(err);
    const isProduction = NODE_ENV === 'production';
    const detail = (safeDetail) => {
        if (isProduction) {
            return safeDetail;
        }

        if (err && err.message) {
            return err.message;
        }

        return safeDetail;
    };

    let customError = err;

    if (!(err instanceof BaseError)) {
        if (err && err.name === 'ValidationError') {
            customError = new RequestValidationError(detail('Invalid request payload'));
        } else if (err && err.code === 11000) {
            const field = Object.keys(err.keyPattern || err.keyValue || {})[0] || 'field';
            customError = new ConflictError(`${field} already exists`, detail('Duplicate value'));
        } else if (err && err.name === 'JsonWebTokenError') {
            customError = new Unauthorized('Invalid token', detail('Invalid token'));
        } else if (err && err.name === 'TokenExpiredError') {
            customError = new Unauthorized('Token expired', detail('Token expired'));
        } else {
            customError = new InternalServerError(detail('Internal server error'));
        }
    }

    return res.status(customError.statusCode).json(customError.toResponsePayload());
}

module.exports = errorHandler;
