'use strict';

const API = require('../../app/utils/API');
const assert = require('power-assert');
const nock = require('nock');

suite('API', () => {
  suite('._buildUrl()', () => {
    test('should return uri as is', () => {
      const uri = new API()._buildUrl('/repos/sullenor/hello/collaborators');

      assert.equal(uri, '/repos/sullenor/hello/collaborators');
    });

    test('should fullfil the url', () => {
      const uri = new API({uri: 'https://github.yandex-team.ru/api/v3/'})
        ._buildUrl('/repos/sullenor/hello/collaborators');

      assert.equal(uri, 'https://github.yandex-team.ru/api/v3/repos/sullenor/hello/collaborators');
    });

    test('should respect the user choise', () => {
      const uri = new API({uri: 'https://github.yandex-team.ru/api/v3/'})
        ._buildUrl('https://github.com/api/v3/repos/sullenor/hello/collaborators');

      assert.equal(uri, 'https://github.com/api/v3/repos/sullenor/hello/collaborators');
    });
  });

  suite('.get()', () => {
    suite('handling errors', () => {
      setup(() => nock('https://github.com/')
        .get('/api/v3/repos/sullenor/hello/collaborators')
        .reply(404, 'Not Found'));

      teardown(() => nock.cleanAll());

      test('should handle error', done => {
        new API().get('https://github.com/api/v3/repos/sullenor/hello/collaborators')
          .then(response => done(new Error('failed')))
          .catch(er => {
            assert.equal(
              er.message,
              '404 Not Found ← GET https://github.com/api/v3/repos/sullenor/hello/collaborators'
            );
            done();
          });
      });
    });

    suite('handling responses', () => {
      setup(() => nock('https://github.com/')
        .get('/api/v3/repos/sullenor/hello/collaborators')
        .reply(200, {
          result: 'ok',
        }, {
          'content-type': 'application/json; charset=utf-8',
        }));

      teardown(() => nock.cleanAll());

      test('should receive response', done => {
        new API().get('https://github.com/api/v3/repos/sullenor/hello/collaborators')
          .then(response => {
            assert(typeof response === 'object');
            assert.equal(response.result, 'ok');
            done();
          })
          .catch(er => done(er));
      });
    });
  });

  suite('.post()', () => {
    suite('handling errors', () => {
      setup(() => nock('https://github.com/')
        .post('/api/v3/repos/sullenor/hello/collaborators')
        .reply(404, 'Not Found'));

      teardown(() => nock.cleanAll());

      test('should handle error', done => {
        new API().post('https://github.com/api/v3/repos/sullenor/hello/collaborators')
          .then(response => done(new Error('failed')))
          .catch(er => {
            assert.equal(er.message, '404 Not Found ← POST https://github.com/api/v3/repos/sullenor/hello/collaborators');
            done();
          });
      });
    });

    suite('handling responses', () => {
      setup(() => nock('https://github.com/')
        .post('/api/v3/repos/sullenor/hello/collaborators')
        .reply(200, {
          result: 'ok',
        }, {
          'content-type': 'application/json; charset=utf-8',
        }));

      teardown(() => nock.cleanAll());

      test('should receive response', done => {
        new API().post('https://github.com/api/v3/repos/sullenor/hello/collaborators')
          .then(response => {
            assert(typeof response === 'object');
            assert.equal(response.result, 'ok');
            done();
          })
          .catch(er => done(er));
      });
    });
  });
});
