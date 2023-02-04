'use strict';

const assert = require('power-assert');
const pullRequest = require('../../app/stories/pullRequest');

const EVENT = require('../fixture/pullRequestEvent');

suite('pullRequest', () => {
  suite('.extractContext()', () => {
    test('should work', () => {
      assert.deepEqual(pullRequest.extractContext(EVENT), {
        action: 'synchronize',
        author: 'alex-vee',
        branch: 'INFRASTRUCTUREPI-555_alex-vee_creating_betas_from_jenkins_artifacts',
        description: 'Тестовый пулл реквест',
        merged: false,
        user: 'alex-vee',
        number: 2,
        owner: 'partner',
        repo: 'yharnam',
        url: 'https://github.yandex-team.ru/partner/yharnam/pull/2',
      });
    });
  });
});
