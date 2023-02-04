'use strict';

const {isEqual} = require('lodash');
const assert = require('power-assert');
const randomize = require('../../app/utils/randomize');

let uniqQId = Date.now();

suite('randomize', () => {
  suite('randomize()', () => {
    test('should shuffle elements', () => {
      const col = [1, 2, 3, 4, 5];
      const result = col.map(() => randomize(String(uniqQId++), col, 6));

      assert.ok(!isEqual(col, result));
    });

    test('should shuffle elements', () => {
      const col = [1, 2, 3, 4, 5];
      const result = col.map(() => randomize(String(uniqQId++), col, 6));

      assert.ok(!isEqual(col, result));
    });

    test('should return elem', () => {
      assert.equal(randomize(String(uniqQId++), [1], 2), 1);
      assert.equal(randomize(String(uniqQId++), [1, 2], 1), 2);
      assert.equal(randomize(String(uniqQId++), [1, 2], 2), 1);
      assert.equal(randomize(String(uniqQId++), [3, 2], 2), 3);
      assert.equal(randomize(String(uniqQId++), [1, 1, 1, 1, 1, 1], 5), 1);
    });

    test('should respect exception', () => {
      const col = [1, 3, 5];
      const variants = [];
      variants.push(randomize(String(uniqQId++), col, 1));
      variants.push(randomize(String(uniqQId++), col, 1));
      variants.push(randomize(String(uniqQId++), col, 1));
      variants.push(randomize(String(uniqQId++), col, 1));
      variants.push(randomize(String(uniqQId++), col, 1));
      variants.push(randomize(String(uniqQId++), col, 1));
      assert.equal(variants.includes(1), false);
    });
  });
});
