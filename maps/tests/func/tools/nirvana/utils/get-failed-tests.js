const dayData = require('../../../reports/hermione/day/data');
const nightData = require('../../../reports/hermione/night/data');

function getBrowsersBySuite(suite) {
    if (suite.browsers) {
        return suite.browsers.map((browser) => {
            browser.name = suite.suitePath.slice(1).join(' / ');

            return browser;
        });
    }

    return suite.children.reduce((res, curr) => res.concat(getBrowsersBySuite(curr)), []);
}

function parseTestsTree(tree) {
    let failedTestsCount = 0;
    let erroredTestsCount = 0;

    const result = tree.suites
        .reduce((res, curr) => res.concat(getBrowsersBySuite(curr)), [])
        .filter((test) => {
            if (test.result.error) {
                erroredTestsCount++;

                console.error(`${test.name} error: ${test.result.error.message}`);
            }

            return !test.result.error && test.result.status === 'fail';
        });

    console.table({
        total: tree.total,
        passed: tree.passed,
        retries: tree.retries
    });

    return result;
}

function getFailedTests() {
    const failedDayTests = parseTestsTree(dayData);
    const failedNightTests = parseTestsTree(nightData);

    return failedDayTests
        .concat(failedNightTests)
        .sort((a, b) => a.name.localeCompare(b.name));
}

function getTestsStats() {
    return {
        day: {
            total: dayData.total,
            failed: dayData.failed,
            retries: dayData.retries
        },
        night: {
            total: nightData.total,
            failed: nightData.failed,
            retries: nightData.retries
        }
    };
}

module.exports = {getFailedTests, getTestsStats};
