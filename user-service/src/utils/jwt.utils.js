const jwt = require('jsonwebtoken');
const { JWT_SECRET, JWT_EXPIRES_IN, JWT_REFRESH_SECRET, JWT_REFRESH_EXPIRES_IN } = require('../config/server.config');
const unauthorizedError = require('../errors/unauthorized.error');

function signToken(payload, secret, expiresIn) {
    return jwt.sign(payload, secret, { expiresIn });
}

function verifyToken(token, secret, errorMessage) {
    try {
        return jwt.verify(token, secret);
    } catch (error) {
        throw new unauthorizedError(errorMessage);
    }
}

/**
 * Generate access token
 * @param {Object} payload - User data to include in token
 * @returns {string} - JWT access token
 */

function generateAccessToken(payload) {
    return signToken(payload, JWT_SECRET, JWT_EXPIRES_IN);
}

/**
 * Generate refresh token
 * @param {Object} payload - User data to include in token
 * @returns {string} - JWT refresh token
 */
function generateRefreshToken(payload) {
    return signToken(payload, JWT_REFRESH_SECRET, JWT_REFRESH_EXPIRES_IN);
}

/**
 * Verify access token
 * @param {string} token - JWT access token
 * @returns {Object} - Decoded token payload
 */
function verifyAccessToken(token) {
    return verifyToken(token, JWT_SECRET, 'Invalid or expired access token');
}

/**
 * Verify refresh token
 * @param {string} token - JWT refresh token
 * @returns {Object} - Decoded token payload
 */
function verifyRefreshToken(token) {
    return verifyToken(token, JWT_REFRESH_SECRET, 'Invalid or expired refresh token');
}

module.exports = {
    generateAccessToken,
    generateRefreshToken,
    verifyAccessToken,
    verifyRefreshToken
};
