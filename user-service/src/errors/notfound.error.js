const BaseError = require('./base.error');
const { StatusCodes } = require('http-status-codes');

class NotFound extends BaseError {
    constructor(resourceName, resourceValue) {
        super(
            'NotFound',
            StatusCodes.NOT_FOUND,
            `${resourceName} not found with value: ${resourceValue}`,
            resourceValue
        );
    }
}

module.exports = NotFound;
