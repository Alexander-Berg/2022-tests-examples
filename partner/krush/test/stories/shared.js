'use strict';

const assert = require('power-assert');
const nock = require('nock');
const Github = require('../../app/utils/Github');
const Startrack = require('../../app/utils/Startrack');
const shared = require('../../app/stories/shared');

const gh = new Github();
const st = new Startrack();

suite('shared', () => {
  teardown(() => nock.cleanAll());

  suite('.chooseReviewer()', () => {
    suite('should provide a member for existing teams', () => {
      setup(() => {
        nock('https://github.yandex-team.ru/')
          .post('/api/v3/repos/partner/yharnam/issues/2/comments')
          .reply(200, require('../fixture/createAComment'));
      });

      test('should work', () => {
        const reviewer = shared.chooseReviewer({
          author: 'chryse',
          number: 2,
          owner: 'partner',
          repo: 'yharnam',
        });

        assert(typeof reviewer === 'string');
      });
    });

    suite('should do nothing for unknown repo', () => {
      test('should work', () => {
        const reviewer = shared.chooseReviewer({
          author: 'pereiro',
          number: 2,
          owner: 'partner',
          repo: 'unknown',
        });

        assert.equal(reviewer, null);
      });
    });
  });

  suite('.provideInformationAboutPullRequestToTicket()', () => {
    setup(() => {
      nock('https://st-api.yandex-team.ru/')
        .post('/v2/issues/INFRASTRUCTUREPI-613/comments')
        .reply(200, require('../fixture/issueCreateAComment'));
    });

    test('should work', done => {
      shared.provideInformationAboutPullRequestToTicket(null, st, {
        branch: 'INFRASTRUCTUREPI-613-some-branch',
        repo: 'yharnam',
        url: 'some url',
      })
        .then(response => {
          assert.equal(response.text, '%%small test%%\napi');
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.resetCodeReviewStatus()', () => {
    setup(() => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/pulls/2/commits')
        .reply(200, require('../fixture/listCommitsOnAPullRequest'))
        .post('/api/v3/repos/partner/yharnam/statuses/9d8e3daca515a1c01a1d7889b98e913eaf88cd93')
        .reply(200, require('../fixture/createAStatus'));
    });

    test('should work', done => {
      shared.resetCodeReviewStatus(gh, {
        author: 'alex-vee',
        number: 2,
        owner: 'partner',
        repo: 'yharnam',
      })
        .then(response => {
          assert.equal(response.state, 'success');
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.updateCodeReviewStatus()', () => {
    setup(() => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/issues/2/labels')
        .reply(200, require('../fixture/listLabelsOnAnIssue'))
        .get('/api/v3/repos/partner/yharnam/pulls/2/commits')
        .reply(200, require('../fixture/listCommitsOnAPullRequest'))
        .get('/api/v3/repos/partner/yharnam/issues/2/comments?per_page=100')
        .reply(200, require('../fixture/listCommentsOnAnIssue'))
        .post('/api/v3/repos/partner/yharnam/statuses/9d8e3daca515a1c01a1d7889b98e913eaf88cd93', {
          state: 'pending',
          context: 'Code review',
          description: 'Результат голосования: 0',
        })
        .reply(200, require('../fixture/createAStatus'))
        .get('/api/v3/repos/partner/yharnam/labels')
        .reply(200, require('../fixture/listAllLabelsForThisRepository'))
        .get('/api/v3/repos/partner/yharnam/issues/2/labels')
        .reply(200, require('../fixture/listLabelsOnAnIssue'))
        .post('/api/v3/repos/partner/yharnam/labels')
        .reply(200, [])
        .post('/api/v3/repos/partner/yharnam/issues/2/labels')
        .reply(200, []);
    });

    test('should work', done => {
      shared.updateCodeReviewStatus(gh, {
        author: 'alex-vee',
        number: 2,
        owner: 'partner',
        repo: 'yharnam',
      })
        .then(response => {
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('.updateLabels()', () => {
    setup(() => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/labels')
        .reply(200, require('../fixture/listAllLabelsForThisRepository'))
        .get('/api/v3/repos/partner/yharnam/issues/2/labels')
        .reply(200, require('../fixture/listLabelsOnAnIssue'))
        .post('/api/v3/repos/partner/yharnam/labels')
        .reply(200, [])
        .post('/api/v3/repos/partner/yharnam/issues/2/labels')
        .reply(200, []);
    });

    test('should work', done => {
      shared.updateLabels(gh, {
        number: 2,
        owner: 'partner',
        repo: 'yharnam',
        members: ['alex-vee', 'sullenor'],
      })
        .then(response => {
          assert.deepEqual(response, []);
          done();
        })
        .catch(er => done(er));
    });
  });

  suite('get free port for beta', () => {
    setup(() => {
      nock('https://creator-myt.partner.yandex-team.ru/')
          .get('/api/3/betas/')
          .reply(200, require('../fixture/listBetas'))
    });

    test('should work', () => {
      shared.getFreePort()
          .then(port => {
            assert.equal(port, 8511);
          });
    });
  });

  suite('get last image DB for beta', () => {
    setup(() => {
      nock('https://creator-myt.partner.yandex-team.ru/')
          .get('/api/3/cached_docker_db_images')
          .reply(200, require('../fixture/listImagesDB'))
    });

    test('should work', () => {
      shared.getLastImageDB()
          .then(imageDB => {
            assert.equal(imageDB, 'registry.partner.yandex-team.ru/partner2-db-general-2.18.1287-2018-12-31');
          });
    });
  });
});
