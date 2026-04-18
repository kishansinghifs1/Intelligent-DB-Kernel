const BaseError = require('./base.error');
const { StatusCodes } = require('http-status-codes');

class ConflictError extends BaseError {
    constructor(message = 'Resource conflict', details) {
        super(
            'ConflictError',
            StatusCodes.CONFLICT,
            message,
            details
        );
    }
}

module.exports = ConflictError;
