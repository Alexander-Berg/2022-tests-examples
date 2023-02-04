/* global describe, before, after, it, afterEach, beforeEach, require */
const request = require('supertest');
const proxyquire = require('proxyquire-2');

const stubs = {
    './lib/utils': {
        addYaInternalCA: () => {},
        getTvmLocalAuthToken: () => 'fake-token',
    },
};

const app = proxyquire('../app', stubs);

describe('GET /ping', () => {
    before(() => {

    });

    after(() => {

    });

    beforeEach(() => {

    });

    afterEach(() => {

    });

    it('respond with OK', (done) => {
        request(app)
            .get('/ping')
            .expect(200, done);
    });
});
