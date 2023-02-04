import testCases from './web-maps-cases.json';
import {Tracker} from '@yandex-int/maps._tracker/out/types';
import {calculateIssueSeverity} from '../src/zbp';
import {getConfig} from '../src/config';

describe('scripts/zbp', () => {
    describe('calculateIssueSeverity (web-maps)', () => {
        const config = getConfig('web-maps');
        testCases.forEach((testCase) => {
            const title = testCase.title || `should calculate right severity for ${JSON.stringify(testCase.issue)}`;
            const issue = testCase.issue as Tracker.Issue;
            it(title, () => {
                expect(calculateIssueSeverity(issue, config)).toBe(testCase.severity);
            });
        });
    });
});
