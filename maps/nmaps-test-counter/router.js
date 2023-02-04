const {Router} = require('express');
const getCounter = require('./get-counter');

module.exports = Router()
    .get('/', getCounter);
