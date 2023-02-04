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

    describe('ugc upload', () => {
        it('should send request to ugc', (done) => {
            supertest(app)
                .post('/api/v1/ugc-photo')
                .end(() => {
                    sinon.assert.calledWith(request.post, {
                        url: 'http://ugc-test.n.yandex-team.ru/upload2-internal?'
                    });
                    done();
                });
        });

        it('should proxy appId parameter to ugc', (done) => {
            supertest(app)
                .post('/api/v1/ugc-photo?appId=maps-beta')
                .end(() => {
                    sinon.assert.calledWith(request.post, {
                        url: 'http://ugc-test.n.yandex-team.ru/upload2-internal?appId=maps-beta'
                    });
                    done();
                });
        });
    });
});
