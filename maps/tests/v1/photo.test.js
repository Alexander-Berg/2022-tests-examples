'use strict';

const supertest = require('supertest');
const request = require('request');
const sinon = require('sinon');

const app = require('../../src/app');
const sandbox = sinon.createSandbox();

describe('photo', () => {
    beforeEach(() => {
        sandbox.spy(request, 'post');
    });

    afterEach(() => {
        sandbox.restore();
    });

    describe('avatars upload', () => {
        it('should respond 400 without "namespace" query param', (done) => {
            supertest(app)
                .post('/api/v1/photo')
                .expect(400, done);
        });

        it('should send request to avatars', (done) => {
            supertest(app)
                .post('/api/v1/photo?namespace=tycoon')
                .end(() => {
                    sinon.assert.calledWith(request.post, {
                        url: 'http://avatars-int.mdst.yandex.net:13000/put-tycoon/?'
                    });
                    done();
                });
        });

        it('should proxy expire parameter to avatars', (done) => {
            supertest(app)
                .post('/api/v1/photo?namespace=tycoon&expire=400d')
                .end(() => {
                    sinon.assert.calledWith(request.post, {
                        url: 'http://avatars-int.mdst.yandex.net:13000/put-tycoon/?expire=400d'
                    });
                    done();
                });
        });
    });
});
