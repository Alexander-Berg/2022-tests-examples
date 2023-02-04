const common = require('./common');
const tests = require('../tests-generator');

describe('admin', () => {
    describe('change-person', () => {
        tests(common);
    });
});
