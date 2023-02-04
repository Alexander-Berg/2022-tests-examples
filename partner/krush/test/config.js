'use strict';

const assert = require('power-assert');
const config = require('../app/config');

suite('config', () => {
  test('should have githubToken property', () => {
    assert(config.hasOwnProperty('githubToken'));
  });

  test('should have startrackToken property', () => {
    assert(config.hasOwnProperty('startrackToken'));
  });

  test('should have port property', () => {
    assert(config.hasOwnProperty('port'));
    assert(typeof config.port === 'number');
  });

  test('should have repo property', () => {
    assert(config.hasOwnProperty('repo'));
    assert(typeof config.repo === 'object');
  });

  test('should have servant property', () => {
    assert(config.hasOwnProperty('servant'));
    assert(config.servant === 'robot-pereiro');
  });

  test('should have estimation property', () => {
    assert(config.hasOwnProperty('estimation'));
    assert(typeof config.estimation === 'object');
  });

  test('should have teams property', () => {
    assert(config.hasOwnProperty('teams'));
    assert(typeof config.teams === 'object');
  });

  test('should have workers property', () => {
    assert(config.hasOwnProperty('workers'));
    assert(typeof config.workers === 'number');
  });
});
