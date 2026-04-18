const mongoose = require('mongoose');
const { ATLAS_DB_URL, NODE_ENV } = require('./server.config');

// Helper function to build connection hints based on error types
function buildConnectionHint(error) {
    if (!error) return 'No error information available';

    if (error.name === 'MongoNetworkError') {
        return 'Check your network connection and MongoDB server status';
    }
    if (error.name === 'MongoParseError') {
        return 'Verify that your ATLAS_DB_URL is correctly formatted';
    }
    if (error.name === 'MongoServerSelectionError') {
        return 'Ensure your MongoDB server is running and accessible';
    }
    if (error.name === 'MongoTimeoutError') {
        return 'The connection attempt timed out. Check your network and MongoDB server performance';
    }
    return 'Refer to the error message for more details';
}

// Helper function to convert environment variable to a positive integer with a fallback
function toPositiveInteger(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

const MONGODB_CONNECT_TIMEOUT_MS = toPositiveInteger(process.env.MONGODB_CONNECT_TIMEOUT_MS, 20000);


class DBConnection {
    constructor() {
        if (DBConnection.instance) {
            return DBConnection.instance;
        }
        this.isConnected = false;
        DBConnection.instance = this;
    }

    async connect() {
        if (this.isConnected) {
            console.log('DB Connection: Using existing connection');
            return;
        }
        try {
            if (!ATLAS_DB_URL) {
                throw new Error('ATLAS_DB_URL is not configured');
            }

            await mongoose.connect(ATLAS_DB_URL, {
                serverSelectionTimeoutMS: MONGODB_CONNECT_TIMEOUT_MS,
                connectTimeoutMS: MONGODB_CONNECT_TIMEOUT_MS,
                socketTimeoutMS: MONGODB_CONNECT_TIMEOUT_MS
            });

            this.isConnected = true;

            console.log(`DB Connection: New connection established (${NODE_ENV || 'unknown'})`);
            
        } catch (error) {
            const hint = buildConnectionHint(error);
            console.error('Unable to connect to the DB server');
            console.error(`MongoDB startup error: ${error?.name || 'Error'} - ${error?.message || error}`);
            console.error(`MongoDB troubleshooting hint: ${hint}`);
            throw error;
        }
    }

    async disconnect() {
        if (this.isConnected) {
            await mongoose.disconnect();
            this.isConnected = false;
            console.log('DB Connection: Disconnected');
        }
    }
}

const dbConnection = new DBConnection();
module.exports = dbConnection;
