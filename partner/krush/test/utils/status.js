'use strict';

const assert = require('power-assert');
const forEach = require('lodash').forEach;
const status = require('../../app/utils/status');
const STATES = status.STATES;

suite('status', () => {
  suite('.createStatus()', () => {
    forEach(STATES, (state, prop) => {
      test(`should return a status for "${state}" state`, () => {
        const result = status.createStatus('context', STATES[prop]);

        assert.ok(typeof result === 'object', 'status should be an object');
        assert.equal(result.state, state);
        assert.equal(result.context, 'context');
      });
    });

    test('should throw an error for the invalid state', () => {
      assert.throws(() => {
        status.createStatus('context', 'what it should be?');
      }, /Unknown state/);
    });
  });

  suite('.createCodeReviewStatus()', () => {
    test('should has context "Code review"', () => {
      const result = status.createCodeReviewStatus(STATES.SUCCESS, 'description');

      assert.ok(typeof result === 'object', 'status should be an object');
      assert.equal(result.context, 'Code review');
      assert.equal(result.description, 'description');
      assert.equal(result.state, 'success');
    });
  });
});
