'use strict';

const supertest = require('supertest');
const sinon = require('sinon');
const {S3} = require('aws-sdk');

const app = require('../../src/app');
const sandbox = sinon.createSandbox();

describe('create-feedback-photo-url', () => {
    beforeEach(() => {
        sandbox.stub(S3.prototype, 'upload').callsFake(({Bucket, Key}) => {
            return {
                promise: () => Promise.resolve({Location: `http://${Bucket}.s3.mdst.yandex.net/${Key}`})
            };
        });
    });

    afterEach(() => {
        sandbox.restore();
    });

    it('should respond 400 with empty body', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .expect('Bad request')
            .expect(400, done);
    });

    it('should respond 200 with location url contains hash by file content', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .attach('file', 'tests/files/logo.jpg')
            .expect('{"url":"http://maps-feedback-int.s3.mdst.yandex.net/4cb151e45681018b332c7660c333df13"}')
            .expect(200, done);
    });

    it('should respond 400 with many field of "file" type', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .attach('file', 'tests/files/logo.jpg')
            .attach('anotherFile', 'tests/files/logo.jpg')
            .expect('Bad request')
            .expect(400, done);
    });

    it('should respond 400 with not file content allowed', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .attach('file', 'tests/files/fake-image.jpg')
            .expect('Bad request')
            .expect(400, done);
    });

    it('should respond 400 with file big size(more than 5mb)', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .attach('file', 'tests/files/big-cat.jpg')
            .expect('Bad request')
            .expect(400, done);
    });

    it('should respond 400 with field not of "file" type', (done) => {
        supertest(app)
            .post('/api/v1/create-feedback-photo-url')
            .field('description', 'blablabla')
            .expect('Bad request')
            .expect(400, done);
    });
});
