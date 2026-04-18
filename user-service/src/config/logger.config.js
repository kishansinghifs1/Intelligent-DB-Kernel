const winston = require('winston');
const { ATLAS_DB_URL, NODE_ENV } = require('./server.config');

const allowedTransports = [];

allowedTransports.push(new winston.transports.Console({
    format: winston.format.combine(
        winston.format.colorize(),
        winston.format.timestamp({
            format: 'YYYY-MM-DD HH:mm:ss'
        }),
        winston.format.printf((log) => `${log.timestamp} [${log.level}]: ${log.message}`)
    )
}));


allowedTransports.push(new winston.transports.File({
    filename: 'app.log'
}));

allowedTransports.push(new winston.transports.File({
    filename: 'error.log',
    level: 'error'
}));


const logger = winston.createLogger({
    format: winston.format.combine(
        winston.format.timestamp({
            format: 'YYYY-MM-DD HH:mm:ss'
        }),
        winston.format.errors({ stack: true }),
        winston.format.splat(),
        winston.format.json()
    ),
    transports: allowedTransports
});

module.exports = logger;
