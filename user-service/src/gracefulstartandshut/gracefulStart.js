const connectToDB = require('../config/db.config');
const redisClient = require('../config/redis.config');


// Graceful start of the server by connecting the db

async function startServer(app,PORT) {
    try {
        await connectToDB.connect();
        await redisClient.connect();
        app.listen(PORT, () => {
            console.log(`Server started at PORT: ${PORT}`);
        });
    } catch (error) {
        console.error(`Startup failed: service cannot start -> error: ${error?.name || 'Error'} - ${error?.message || error}`);
        process.exit(1);
    }
}

module.exports = startServer