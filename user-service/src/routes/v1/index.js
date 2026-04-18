const express = require('express');
const userRouter = require('./user.routes');

const v1Router = express.Router();

// If any request comes and route continues with /users, we map it to userRouter
v1Router.use('/users', userRouter);

module.exports = v1Router;
