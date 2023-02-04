'use strict';

const request = require('supertest');
const app = require('../src/app');

describe('the basic interface of the HTTP API', () => {
    it('should respond with 200 on /ping requests', (done) => {
        request(app)
            .get('/ping')
            .expect(200, done);
    });

    it('should respond with 404 on unsupported paths', (done) => {
        request(app)
            .get('/cat_pictures')
            .expect(404, done);
    });

    it('should be free of X-Powered-By headers', (done) => {
        request(app)
            .get('/ping')
            .end((err, res) => {
                if (res.headers['x-powered-by']) {
                    return done(new Error('X-Powered-By header should be disabled'));
                }
                done();
            });
    });
});
