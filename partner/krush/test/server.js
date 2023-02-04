'use strict';

const krush = require('../app/server');
const nock = require('nock');
const request = require('supertest');

suite('server', () => {
  suite('issueCommentEvent', () => {
    setup(done => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/issues/6/labels')
        .reply(200, require('./fixture/listLabelsOnAnIssue'))
        .get('/api/v3/repos/partner/yharnam/issues/6/comments?per_page=100')
        .reply(200, require('./fixture/listCommentsOnAnIssue'))
        .get('/api/v3/repos/partner/yharnam/pulls/6/commits')
        .reply(200, require('./fixture/listCommitsOnAPullRequest'))
        .post('/api/v3/repos/partner/yharnam/statuses/9d8e3daca515a1c01a1d7889b98e913eaf88cd93')
        .reply(200, require('./fixture/createAStatus'))
        .get('/api/v3/repos/partner/yharnam/labels')
        .reply(200, require('./fixture/listAllLabelsForThisRepository'))
        .get('/api/v3/repos/partner/yharnam/issues/6/labels')
        .reply(200, require('./fixture/listLabelsOnAnIssue'))
        .post('/api/v3/repos/partner/yharnam/labels')
        .twice()
        .reply(200, [])
        .post('/api/v3/repos/partner/yharnam/issues/6/labels')
        .reply(200, []);

      request(krush)
        .post('/webhooks')
        .set('content-type', 'application/json')
        .set('user-agent', 'supertest')
        .set('x-github-delivery', '75c23b00-c8b4-11e5-887e-a37369b5aef0')
        .set('x-github-event', 'issue_comment')
        .send(require('./fixture/issueCommentEvent'))
        .end(() => done());
    });

    test('should manage', () => {});
  });

  suite('pullRequestEvent', () => {
    setup(done => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/pulls/6/commits')
        .reply(200, require('./fixture/listCommitsOnAPullRequest'))
        .get('/api/v3/repos/partner/yharnam/teams')
        .reply(200, require('./fixture/listTeams'))
        .get('/api/v3/teams/892/members')
        .reply(200, require('./fixture/listTeamMembers'))
        .post('/api/v3/repos/partner/yharnam/issues/6/assignees')
        .reply(200)
        .post('/api/v3/repos/partner/yharnam/statuses/9d8e3daca515a1c01a1d7889b98e913eaf88cd93')
        .reply(200, require('./fixture/createAStatus'))
        .post('/api/v3/repos/partner/yharnam/issues/6/comments')
        .reply(200, require('./fixture/createAComment'))
        .post('/api/v3/repos/partner/yharnam/issues/6/comments')
        .reply(200, require('./fixture/createAComment'));

      nock('https://st-api.yandex-team.ru/')
        .get('/v2/issues/PI-5447')
        .reply(200, require('./fixture/issues_PI-5447'))
        .post('/v2/issues/PI-5447/comments')
        .reply(200, require('./fixture/issueCreateAComment'));

      nock('https://creator-myt.partner.yandex-team.ru/')
        .get('/api/3/betas/')
        .reply(200, require('./fixture/listBetas'))
        .get('/api/3/cached_docker_db_images')
        .reply(200, require('./fixture/listImagesDB'))
        .put('/api/3/betas/8511')
        .reply(200, require('./fixture/createBeta'));

      nock('https://st-api.yandex-team.ru/')
          .post('/v2/issues/PI-5447/comments')
          .reply(200, require('./fixture/issueCreateAComment'));

      request(krush)
        .post('/webhooks')
        .set('content-type', 'application/json')
        .set('user-agent', 'supertest')
        .set('x-github-delivery', '00ed7f00-c351-11e5-8085-5c60fa608710')
        .set('x-github-event', 'pull_request')
        .send(require('./fixture/pullRequestEvent2'))
        .end(() => done());
    });

    test('should manage', () => {});
  });

  suite('pullRequestEvent_merge', () => {
    setup(done => {
      nock('https://st-api.yandex-team.ru/')
        .post('/v2/issues/INFRASTRUCTUREPI-1036/transitions/merge/_execute')
        .reply(200, require('./fixture/issues_INFRASTRUCTUREPI-1036'));

      request(krush)
        .post('/webhooks')
        .set('content-type', 'application/json')
        .set('user-agent', 'supertest')
        .set('x-github-delivery', '00ed7f00-c351-11e5-8085-5c60fa608710')
        .set('x-github-event', 'pull_request')
        .send(require('./fixture/pullRequestEvent_merge'))
        .end(() => done());
    });

    test('should manage', () => {});
  });

  suite('status', done => {
    setup(done => {
      nock('https://github.yandex-team.ru/')
        .get('/api/v3/repos/partner/yharnam/pulls')
        .reply(200, require('./fixture/listPullRequests'))
        .get('/api/v3/repos/partner/yharnam/commits/2602687e9e44b99bec43e69102f2a8d8d2cbbc80/status')
        .reply(200, require('./fixture/getTheCombinedStatus'));

      nock('https://st-api.yandex-team.ru/')
        .get('/v2/issues/PI-6889')
        .reply(200, require('./fixture/issues_PI-6889'))
        .post('/v2/issues/PI-6889/transitions/resolve/_execute')
        .reply(200);

      request(krush)
        .post('/webhooks')
        .set('content-type', 'application/json')
        .set('user-agent', 'supertest')
        .set('x-github-delivery', '75c23b00-c8b4-11e5-887e-a37369b5aef0')
        .set('x-github-event', 'status')
        .send(require('./fixture/statusEvent'))
        .end(() => done());
    });

    test('should manage', () => {});
  });
});
