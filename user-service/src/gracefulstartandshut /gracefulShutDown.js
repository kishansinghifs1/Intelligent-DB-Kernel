const { set } = require('mongoose');
const connectToDB = require('../config/db.config');
const redisClient = require('../config/redis.config');


// Graceful Shutdown by disconnecting the db

async function shutdown(signal) {
    console.log(`Received ${signal}. Shutting down gracefully...`);
    const result = await Promise.allSettled([
        connectToDB.disconnect(),
        redisClient.disconnect()
    ]);

    result.forEach((res, index) => {
        if (res.status === 'fulfilled') {
            console.log(`Resource ${index + 1} disconnected successfully.`);
        } else {
            console.error(`Error disconnecting resource ${index + 1}:`, res.reason);
        }
    });

    console.log('Server shutdown complete');

    setTimeout(() => {
    process.exit(0);
    }, 1000);
}

module.exports = shutdown