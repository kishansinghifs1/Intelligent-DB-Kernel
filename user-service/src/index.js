const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');

const { PORT } = require('./config/server.config');
const apiRouter = require('./routes');
const errorHandler = require('./utils/errorHandler');

const startServer = require('./gracefulstartandshut/gracefulStart')
const shutdown = require('./gracefulstartandshut/gracefulShutDown');


//initialisation
const app = express();

// Middleware
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.text());


app.use(cors({
    origin: true,
    methods: ['GET', 'PUT', 'POST', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization'], 
}));


// All requests that start with /api will be mapped to apiRouter
app.use('/api', apiRouter);

// Health check endpoint
app.get('/ping', (req, res) => {
    return res.json({ message: 'User Service is alive' });
});

// Global error handler or global catch
app.use(errorHandler);

//Graceful start and shutdown of the server
startServer(app, PORT);

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
