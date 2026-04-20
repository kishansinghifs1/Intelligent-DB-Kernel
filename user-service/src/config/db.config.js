const mongoose = require('mongoose');

const BadRequest = require('../errors/badrequest.error');
const InternalServerError = require('../errors/internalserver.error');

const {
    ATLAS_DB_URL,
    NODE_ENV,
    MONGODB_SERVER_SELECTION_TIMEOUT_MS,
    MONGODB_CONNECT_TIMEOUT_MS,
    MONGODB_SOCKET_TIMEOUT_MS
} = require('./server.config');

class DBConnection {


    //constructor implements a singleton pattern to ensure only one DB connection instance exists
    constructor() {

        if (DBConnection.instance) {
            return DBConnection.instance;
        }

        this.isConnected = false;

        // Store the instance on the class itself to enforce singleton behavior
        DBConnection.instance = this;
    }

    async connect() {

        if (this.isConnected) {
            console.log('DB Connection: Using existing connection');
            return;
        }

        try {
            
            if (!ATLAS_DB_URL) {
                throw new BadRequest(
                    'ATLAS_DB_URL',
                    'Database connection string is missing from environment configuration'
                );
            }

            await mongoose.connect(ATLAS_DB_URL, {
                serverSelectionTimeoutMS: MONGODB_SERVER_SELECTION_TIMEOUT_MS,
                connectTimeoutMS: MONGODB_CONNECT_TIMEOUT_MS,
                socketTimeoutMS: MONGODB_SOCKET_TIMEOUT_MS
            });

            this.isConnected = true;

            console.log(`DB Connection: New connection established (${NODE_ENV || 'unknown'})`);
            
        } catch (error) {

            console.error('Unable to connect to the DB server');
            
            throw new InternalServerError(`${error?.message || 'MongoDB connection failed'}`);
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
