'use strict';

const assert = require('power-assert');
const issueComment = require('../../app/stories/issueComment');

const EVENT = require('../fixture/issueCommentEvent');

suite('issueComment', () => {
  suite('.extractContext()', () => {
    assert.deepEqual(issueComment.extractContext(EVENT), {
      author: 'sullenor',
      body: ':+1: ',
      number: 6,
      owner: 'partner',
      repo: 'yharnam',
      reviewer: 'sullenor',
      url: 'https://github.yandex-team.ru/partner/yharnam/pull/6',
    });
  });
});
