const BaseError = require('./base.error');
const { StatusCodes } = require('http-status-codes');

class ValidationError extends BaseError {
    constructor(details) {
        super(
            'ValidationError',
            StatusCodes.BAD_REQUEST,
            'Validation failed',
            details
        );
    }
}

module.exports = ValidationError;
