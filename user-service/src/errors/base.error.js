class BaseError extends Error {
    constructor(name, statusCode, description, details) {
        super(description);
        this.name = name;
        this.statusCode = statusCode;
        this.details = details;
        Error.captureStackTrace(this);
    }

    toResponsePayload() {
        return {
            success: false,
            message: this.message,
            error: this.details,
            data: {}
        };
    }
}

module.exports = BaseError;
