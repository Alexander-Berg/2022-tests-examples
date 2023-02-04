const result = './reports/tiles-loaded-time-result-1.txt';
const allTestsTimePath = './reports/all-tests-time-1.txt';

const result2 = './reports/tiles-loaded-time-result-2.txt';
const allTestsTimePath2 = './reports/all-tests-time-2.txt';
const Calculate = require('./calculate');

const fs = require('fs');

(function calculateAverageTime() {
    let results = fs.readFileSync(allTestsTimePath, 'utf8', 'a+').trim().split(/\n/).map((val) => val.split(', '));
    results = [].concat(...results).map((val) => Number(val));

    const calculate = new Calculate(results);
    const round = (val) => Math.round(val);

    let avg = round(calculate.simpleAverage());
    let dev = round(calculate.stdDeviation());
    let per25 = round(calculate.percentile(0.25));
    let per50 = round(calculate.percentile(0.5));
    let per75 = round(calculate.percentile(0.75));
    let per85 = round(calculate.percentile(0.85));
    let per95 = round(calculate.percentile(0.95));

    let count = results.length;
    let min = round(Math.min(...results));
    let max = round(Math.max(...results));

    fs.writeFileSync(result, JSON.stringify({avg, dev, per25, per50, per75, per85, per95, min, max, count}));
    console.log('First viewport', {avg, dev, per25, per50, per75, per85, per95, min, max, count});

    let results2 = fs.readFileSync(allTestsTimePath2, 'utf8', 'a+').trim().split(/\n/).map((val) => val.split(', '));
    results2 = [].concat(...results2).map((val) => Number(val));

    const calculate2 = new Calculate(results2);

    avg = round(calculate2.simpleAverage());
    dev = round(calculate2.stdDeviation());
    per25 = round(calculate2.percentile(0.25));
    per50 = round(calculate2.percentile(0.5));
    per75 = round(calculate2.percentile(0.75));
    per85 = round(calculate2.percentile(0.85));
    per95 = round(calculate2.percentile(0.95));

    count = results2.length;
    min = round(Math.min(...results2));
    max = round(Math.max(...results2));

    fs.writeFileSync(result2, JSON.stringify({avg, dev, per25, per50, per75, per85, per95, min, max, count}));
    console.log('After changing center', {avg, dev, per25, per50, per75, per85, per95, min, max, count});
})();
