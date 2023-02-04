'use strict';

const nock = require('nock');
const supertest = require('supertest');

const app = require('../src/app');
const helpers = require('./helpers/helpers');

const endpoint = '/v2';
const query = {
    quality: '1',
    disputedBorders: 'RU',
    lang: 'ru_RU'
};

describe('/v2 endpoint', () => {
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

    it('sends 200 if called with trailing slash', (done) => {
        supertest(app)
            .get(endpoint + '/')
            .query(query)
            .expect(200, done);
    });

    it('sends 200 if every query parameter is in place', (done) => {
        supertest(app)
            .get(endpoint)
            .query(query)
            .expect(200, done);
    });

    it('should add CORS headers if callback is not passed', (done) => {
        supertest(app)
            .get(endpoint)
            .query(query)
            .expect(200)
            .expect('Access-Control-Allow-Origin', '*', done);
    });
});
