const mongoose = require('mongoose');
const { hashPassword } = require('../utils/password.utils');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: [true, 'Username is required'],
        unique: true,
        trim: true,
        minlength: [3, 'Username must be at least 3 characters long'],
        maxlength: [30, 'Username must not exceed 30 characters'],
        match: [/^[a-zA-Z0-9_]+$/, 'Username can only contain letters, numbers, and underscores']
    },
    email: {
        type: String,
        required: [true, 'Email is required'],
        unique: true,
        trim: true,
        lowercase: true,
        //regrex to validate email address format
        match: [/^\S+@\S+\.\S+$/, 'Please provide a valid email address']
    },
    password: {
        type: String,
        required: [true, 'Password is required'],
        minlength: [8, 'Password must be at least 8 characters long']
    },
    firstName: {
        type: String,
        trim: true,
        maxlength: [50, 'First name must not exceed 50 characters']
    },
    lastName: {
        type: String,
        trim: true,
        maxlength: [50, 'Last name must not exceed 50 characters']
    },
    avatarUrl: {
        type: String,
        default: null
    },
    isActive: {
        type: Boolean,
        default: true
    },
    refreshTokenHash: {
        type: String,
        default: null
    },
    refreshTokenExpiresAt: {
        type: Date,
        default: null
    }
}, {
    timestamps: true,
    strict: true
});

//pre check if password is modified, if yes then hash it before saving to the database  and if not then just save the user without hashing the password again
userSchema.pre('save', async function () {
    if (!this.isModified('password')) {
        return;
    }
    this.password = await hashPassword(this.password);
});

//toJSON method to remove the password field from the user object when it is returned as a response
userSchema.methods.toJSON = function () {
    const userObject = this.toObject();
    delete userObject.password;
    return userObject;
};

const User = mongoose.model('User', userSchema);

module.exports = User;
