'use strict';

const assert = require('power-assert');
const status = require('../../app/stories/status');

const EVENT = require('../fixture/statusEvent');

suite('status', () => {
  suite('.extractContext()', () => {
    assert.deepEqual(status.extractContext(EVENT), {
      branches: [
        'PI-6889_fix_tune_lang'
      ],
      owner: 'partner',
      repo: 'yharnam'
    });
  });
});
