const BaseError = require('./base.error');
const { StatusCodes } = require('http-status-codes');

class Unauthorized extends BaseError {
    constructor(message = 'Unauthorized access', details) {
        super(
            'Unauthorized',
            StatusCodes.UNAUTHORIZED,
            message,
            details
        );
    }
}

module.exports = Unauthorized;
