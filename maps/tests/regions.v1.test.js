'use strict';

const nock = require('nock');
const supertest = require('supertest');

const app = require('../src/app');
const helpers = require('./helpers/helpers');

const endpoint = '/1.0/regions.xml';
const query = {
    quality: '1',
    disputedBorders: 'RU',
    lang: 'ru_RU'
};

describe('/1.0/regions.xml endpoint', () => {
    before(() => {
        nock.disableNetConnect();
        nock.enableNetConnect('127.0.0.1');
    });

    after(() => {
        nock.enableNetConnect();
    });

    beforeEach(helpers.stubNetworkResources);

    afterEach(() => {
        nock.cleanAll();
    });

    it('sends 200 if every query parameter is in place', (done) => {
        supertest(app)
            .get(endpoint)
            .query(query)
            .expect(200, done);
    });
});
