'use strict';

const {assert} = require('chai');
const nock = require('nock');
const sinon = require('sinon');
const supertest = require('supertest');

const app = require('../src/app');
const helpers = require('./helpers/helpers');

const endpoint = '/v2';
const query = {
    quality: '1',
    disputedBorders: 'RU',
    lang: 'ru_RU'
};

describe('endpoint', () => {
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

    it('sends 400 if not all required query parameters are provided', (done) => {
        supertest(app)
            .get(endpoint)
            .expect(400, done);
    });

    it('sends 400 if lang is not provided', (done) => {
        supertest(app)
            .get(endpoint)
            .query(Object.assign({}, query, {lang: undefined}))
            .expect(400, done);
    });

    it('sends 400 if disputedBorders is not provided', (done) => {
        supertest(app)
            .get(endpoint)
            .query(Object.assign({}, query, {disputedBorders: undefined}))
            .expect(400, done);
    });

    it('sends 400 if quality is not provided', (done) => {
        supertest(app)
            .get(endpoint)
            .query(Object.assign({}, query, {quality: undefined}))
            .expect(400, done);
    });

    it('should respond with compression', (done) => {
        supertest(app)
            .get(endpoint)
            .set('Accept-Encoding', 'gzip')
            .query(query)
            .expect('Content-Encoding', 'gzip')
            .expect(200, done);
    });

    it('should respond with UTF-8 encoding', (done) => {
        supertest(app)
            .get(endpoint)
            .query(query)
            .expect('Content-Type', /charset=utf-8/, done);
    });

    it('should use Cache-control', (done) => {
        supertest(app)
            .get(endpoint)
            .query(query)
            .expect('Cache-control', `public, max-age=${60 * 60 * 24}`, done);
    });

    it('should correctly respond on JSONP requests', (done) => {
        const callback = 'testCallback';
        const testCallback = sinon.stub();

        supertest(app)
            .get(endpoint)
            .query(Object.assign({callback}, query))
            .expect(200)
            .expect('Content-Type', /javascript/)
            .end((err, {text}) => {
                // eslint-disable-next-line no-eval
                eval(text);
                assert(testCallback.calledOnce);
                assert.strictEqual(testCallback.args[0][0].status, 'success');
                done(err);
            });
    });

    it('should give correct JSONP errors', (done) => {
        const callback = 'testCallback';
        const testCallback = sinon.stub();

        supertest(app)
            .get(endpoint)
            .query({callback})
            .expect(200)
            .expect('Content-type', /javascript/)
            .end((err, {text}) => {
                // eslint-disable-next-line no-eval
                eval(text);
                assert(testCallback.calledOnce);
                assert.strictEqual(testCallback.args[0][0].status, 'error');
                assert.strictEqual(testCallback.args[0][0].code, 400);
                done(err);
            });
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
