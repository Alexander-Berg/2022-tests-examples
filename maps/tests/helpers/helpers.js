'use strict';

const nock = require('nock');

const config = require('../../src/lib/config');
const regionsDataResponse = require('../resources/regionsData');

const helpers = {};

helpers.stubNetworkResources = async () => {
    nock(config.storageUrl)
        .get(() => true)
        .reply(200, regionsDataResponse);
};

module.exports = helpers;
