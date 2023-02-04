'use strict';

const assert = require('power-assert');
const helper = require('../../app/utils/helper');

const COMMENTS = require('../fixture/listCommentsOnAnIssue');
const COMMITS = require('../fixture/listCommitsOnAPullRequest');
const TEAMS = require('../fixture/listTeams');

suite('helper', () => {
  suite('.estimateVotes()', () => {
    const votes = [
      {
        author: 'alex-vee',
        body: ':+1:',
        date: 1455028326000,
        estimation: 1,
      },
      {
        author: 'eandreyf',
        body: ':+1: ',
        date: 1455027651000,
        estimation: 1,
      },
      {
        author: 'sullenor',
        body: ':+1: ',
        date: 1455027651000,
        estimation: 1,
      }
    ];

    test('should return sum', () => {
      assert.equal(helper.estimateVotes(votes), 3);
    });

    test('should exclude particular persons from voting', () => {
      assert.equal(helper.estimateVotes(votes, ['alex-vee']), 2);
    });
  });

  test('.estimateVotes()', () => {
    assert.equal(helper.estimateVotes([
      {
        author: 'alex-vee',
        body: ':+1:',
        date: 1455028326000,
        estimation: 1,
      },
      {
        author: 'eandreyf',
        body: ':+1: ',
        date: 1455027651000,
        estimation: 1,
      }
    ]), 2);
  });

  test('.extractCommentData()', () => {
    assert.deepEqual(helper.extractCommentData(COMMENTS[0]), {
      body: ':+1: ',
      date: 1450262714000,
      reviewer: 'alex-vee',
    });
  });

  test('.extractCommitData()', () => {
    assert.deepEqual(helper.extractCommitData(COMMITS[0]), {
      author: 'nafania',
      date: 1447745528000,
      sha: '892d1793c6a4a59f88c9310e105c16f584a86fce',
    });
  });

  test('.extractIssueId()', () => {
    assert.equal(helper.extractIssueId('my-branch'), null);
    assert.equal(helper.extractIssueId('PI-2342-my-branch'), 'PI-2342');
  });

  test('.extractPage()', () => {
    const partOfLink = '<https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2>; rel="next"';
    assert.deepEqual(helper.extractPage(partOfLink), {
      rel: 'next',
      url: 'https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2',
    });
  });

  test('.extractTeam()', () => {
    assert.deepEqual(helper.extractTeam(TEAMS[0]), {
      name: 'owners',
      id: 888
    });
  });

  test('.filterCommentsByAuthor()', () => {
    const comments = helper.filterCommentsByAuthor(COMMENTS, 'alex-vee');
    assert(COMMENTS.length > comments.length);
  });

  suite('.filterTeams()', () => {
    test('should return same teams', () => {
      assert.deepEqual(helper.filterTeams(TEAMS), [
        {name: 'owners', id: 888},
        {name: 'backend', id: 892},
      ]);
    });

    test('should filter teams', () => {
      assert.deepEqual(helper.filterTeams(TEAMS, ['backend']), [
        {name: 'backend', id: 892},
      ]);
    });
  });

  test('.findLastCommit()', () => {
    assert.deepEqual(helper.findLastCommit(COMMITS), {
      author: 'sullenor',
      date: 1450193182000,
      sha: '9d8e3daca515a1c01a1d7889b98e913eaf88cd93',
    });
  });

  test('.findLastPage()', () => {
    const link = '<https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2>; rel="next", <https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2>; rel="last"';
    assert.equal(helper.findLastPage(link), 'https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2');
  });

  suite('.findLastPage()', () => {
    test('should return null', () => {
      assert.equal(helper.findLastPage(undefined), null);
    });

    test('should return url', () => {
      const link = '<https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2>; rel="next", <https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2>; rel="last"';
      assert.equal(helper.findLastPage(link), 'https://github.yandex-team.ru/api/v3/repositories/21373/pulls/15/commits?page=2');
    });
  });

  suite('.findTeam()', () => {
    const teams = {
      backend: [
        'bessarabov',
        'blizzard',
      ],
      frontend: [
        'alex-panin',
        'alex-vee',
        'nafania',
        'sullenor',
        'zeffirsky',
      ],
    };

    test('should return null for unknown member', () => {
      assert.equal(helper.findTeam(teams, 'aaa'), null);
    });

    test('should return backend for bessarabov', () => {
      assert.equal(helper.findTeam(teams, 'bessarabov'), 'backend');
    });

    test('should return frontend for nafania', () => {
      assert.equal(helper.findTeam(teams, 'nafania'), 'frontend');
    });
  });

  suite('.findTeammates()', () => {
    const backendTeam = [
      'bessarabov',
      'blizzard',
    ];
    const frontendTeam = [
        'alex-panin',
        'alex-vee',
        'nafania',
        'sullenor',
        'zeffirsky',
      ];
    const teams = {
      backend: backendTeam,
      frontend: frontendTeam,
      crossteam: [
        backendTeam,
        frontendTeam
      ]
    };
    const repos = {
      partner2: 'backend',
      yharnam: 'frontend',
      form: 'crossteam',
    };

    test('should provide members', () => {
      assert.deepEqual(helper.findTeammates(teams, repos, 'yharnam'), frontendTeam);
    });

    test('should find team by author', () => {
      assert.deepEqual(helper.findTeammates(teams, repos, 'form', 'blizzard', 'nafania'), frontendTeam);
    });

    test('should provide empty list', () => {
      assert.deepEqual(helper.findTeammates(teams, repos, 'unknown'), []);
    });
  });

  test('.isEstimationComment()', () => {
    const cases = [
      {body: '👍 ', valid: true},
      {body: '👎', valid: true},
      {body: '👎 ', valid: true},
      {body: ':+1:', valid: true},
      {body: ':-1:', valid: true},
      {body: ':+1: ', valid: true},
      {body: ' :+1: ', valid: true},
      {body: 'sticky text:+1: ', valid: true},
      {body: 'small valueable comment :-1:', valid: true},
      {body: 'big awesome comment :+1: ', valid: true},
    ];

    cases.forEach(testCase =>
      assert.ok(
        testCase.valid
          ? helper.isEstimationComment(testCase)
          : !helper.isEstimationComment(testCase),
        `"${testCase.body}"`
      ));
  });

  test('.listVotes()', () => {
    assert.deepEqual(helper.listVotes(COMMENTS), [
      {
        body: ':-1: ',
        date: 1450375033000,
        estimation: -1,
        reviewer: 'sullenor',
      },
      {
        body: ':+1: ',
        date: 1450356333000,
        estimation: 1,
        reviewer: 'zeffirsky',
      },
      {
        body: ':+1: ',
        date: 1450262714000,
        estimation: 1,
        reviewer: 'alex-vee',
      }
    ]);
  });

  test('.measureEstimation()', () => {
    assert.deepEqual(helper.measureEstimation({
      author: 'sullenor',
      body: ':-1: ',
      date: 1450375033000,
    }), {
      author: 'sullenor',
      body: ':-1: ',
      date: 1450375033000,
      estimation: -1,
    });

    assert.deepEqual(helper.measureEstimation({
      author: 'sullenor',
      body: '👎',
      date: 1450375033000,
    }), {
      author: 'sullenor',
      body: '👎',
      date: 1450375033000,
      estimation: -1,
    });

    assert.deepEqual(helper.measureEstimation({
      author: 'sullenor',
      body: '👍 ',
      date: 1450375033000,
    }), {
      author: 'sullenor',
      body: '👍 ',
      date: 1450375033000,
      estimation: 1,
    });
  });
});
