const common = require('./common');
const tests = require('../tests-generator');

describe('user', () => {
    describe('new-persons', () => {
        tests(common);
    });
});
