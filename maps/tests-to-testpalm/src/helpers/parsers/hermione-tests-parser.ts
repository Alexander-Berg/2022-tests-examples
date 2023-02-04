/* eslint-disable no-invalid-this */
import BaseTestsParser from './base-tests-parser';
import {TestsGlobal} from '../../types';

class HermioneTestsParser extends BaseTestsParser {
    private _hermione = {
        skip: {
            in: (platforms: string | RegExp, reason: string) => {
                this._skipNext = {
                    skipIn: String(platforms),
                    skipReason: reason
                };
            }
        },
        only: {
            in: () => {}
        },
        testPalm: {
            setComponent: (component: string) => {
                this._nextComponent = component;
            }
        },
        // https://www.npmjs.com/package/hermione-passive-browsers
        also: {
            in: () => {}
        }
    };

    protected _prepareGlobal(testGlobal: TestsGlobal): void {
        super._prepareGlobal(testGlobal);

        testGlobal.hermione = this._hermione;
    }
}

export default HermioneTestsParser;
