'use strict';

const assert = require('power-assert');
const Startrack = require('../../app/utils/Startrack');
const nock = require('nock');

suite('Startrack', () => {
  teardown(() => {
    nock.cleanAll();
  });

  suite('.get()', () => {
    setup(() => {
      nock('https://st-api.yandex-team.ru/', {reqheaders: {authorization: 'OAuth test_token'}})
        .get('/v2/issues').reply(200, {result: 'ok'});
    });

    test('should receive response', done => {
      new Startrack({uri: 'https://st-api.yandex-team.ru/v2/', token: 'test_token'})
        .get('/issues')
        .then(response => {
          assert.deepEqual(response, {result: 'ok'});
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.post()', () => {
    setup(() => {
      nock('https://st-api.yandex-team.ru/', {reqheaders: {authorization: 'OAuth test_token'}})
        .post('/v2/issues', {request: 'ok'}).reply(200, {result: 'ok'});
    });

    teardown(() => nock.cleanAll());

    test('should receive response', done => {
      new Startrack({uri: 'https://st-api.yandex-team.ru/v2/', token: 'test_token'})
        .post('/issues', {request: 'ok'})
        .then(response => {
          assert.deepEqual(response, {result: 'ok'});
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.getIssue()', () => {
    setup(() => {
      nock('https://st-api.yandex-team.ru/', {reqheaders: {authorization: 'OAuth test_token'}})
        .get('/v2/issues/PI-5447').reply(200, require('../fixture/issues_PI-5447'));
    });

    test('should return information about issue', done => {
      new Startrack({token: 'test_token'})
        .getIssue('PI-5447')
        .then(response => {
          assert(response.description);
          assert(response.createdBy.display);
          done();
        })
        .catch(er => done(er));
    });
  });
});
