const express = require('express');
const v1Router = require('./v1');

const apiRouter = express.Router();

// Map /v1 routes
apiRouter.use('/v1', v1Router);

module.exports = apiRouter;
