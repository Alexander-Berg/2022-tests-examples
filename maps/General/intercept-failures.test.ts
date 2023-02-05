import {ErrorWithMetadata} from './utils/assert';

/* eslint-disable no-invalid-this */
const failures = [];

afterEach(function () {
    if (this.currentTest.state === 'failed') {
        failures.push(this.currentTest.title, (this.currentTest.err as ErrorWithMetadata).metadata);
    }
});

after(() => {
    console.log('Failed checks', failures);
});
