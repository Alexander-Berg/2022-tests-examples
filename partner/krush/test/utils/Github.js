'use strict';

const assert = require('power-assert');
const Github = require('../../app/utils/Github');
const nock = require('nock');

suite('Github', () => {
  teardown(() => nock.cleanAll());

  suite('.get()', () => {
    setup(() => {
      nock('https://github.yandex-team.ru/', {reqheaders: {authorization: 'token test_token'}})
        .get('/api/v3/repos/sullenor/hello/collaborators').reply(200, [{result: 'ok'}]);
    });

    test('should receive response', done => {
      new Github({uri: 'https://github.yandex-team.ru/api/v3/', token: 'test_token'})
        .get('/repos/sullenor/hello/collaborators')
        .then(response => {
          assert.deepEqual(response, [{result: 'ok'}]);
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.post()', () => {
    setup(() => {
      nock('https://github.yandex-team.ru/', {reqheaders: {authorization: 'token test_token'}})
        .post('/api/v3/repos/sullenor/hello/collaborators', {request: 'ok'}).reply(200, [{result: 'ok'}]);
    });

    test('should receive response', done => {
      new Github({uri: 'https://github.yandex-team.ru/api/v3/', token: 'test_token'})
        .post('/repos/sullenor/hello/collaborators', {request: 'ok'})
        .then(response => {
          assert.deepEqual(response, [{result: 'ok'}]);
          done();
        })
        .catch(er => done(er));
    });
  });
});
