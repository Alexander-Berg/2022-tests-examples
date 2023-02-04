const testTimePath = './reports/test-time-1.txt';
const allTestsTimePath = './reports/all-tests-time-1.txt';
const runTimePath = './reports/run-time-1.txt';
const testTimePath2 = './reports/test-time-2.txt';
const allTestsTimePath2 = './reports/all-tests-time-2.txt';
const runTimePath2 = './reports/run-time-2.txt';
const Calculate = require('./calculate');
const fs = require('fs');

(function writeRunTime() {
    const results = fs.readFileSync(testTimePath, 'utf8', 'a+').trim().split(' ').map((val) => Number(val));
    const calculate = new Calculate(results);

    let avg = calculate.simpleAverage();
    let dev = calculate.stdDeviation();
    let per25 = (calculate.percentile(0.25));
    let per50 = (calculate.percentile(0.5));
    let per75 = (calculate.percentile(0.75));
    let per85 = (calculate.percentile(0.85));
    let per95 = (calculate.percentile(0.95));
    let min = Math.min(...results);
    let max = Math.max(...results);
    let count = results.length;

    fs.appendFileSync(allTestsTimePath, results.join(', ') + '\n');

    fs.appendFileSync(runTimePath, JSON.stringify({avg, dev, per25, per50, per75, per85, per95, min, max, count}) + '\n');

    const results2 = fs.readFileSync(testTimePath2, 'utf8', 'a+').trim().split(' ').map((val) => Number(val));
    const calculate2 = new Calculate(results2);

    avg = calculate2.simpleAverage();
    dev = calculate2.stdDeviation();
    per25 = (calculate2.percentile(0.25));
    per50 = (calculate2.percentile(0.5));
    per75 = (calculate2.percentile(0.75));
    per85 = (calculate2.percentile(0.85));
    per95 = (calculate2.percentile(0.95));
    min = Math.min(...results2);
    max = Math.max(...results2);
    count = results.length;

    fs.appendFileSync(allTestsTimePath2, results2.join(', ') + '\n');

    fs.appendFileSync(runTimePath2, JSON.stringify({avg, dev, per25, per50, per75, per85, per95, min, max, count}) + '\n');
})();
