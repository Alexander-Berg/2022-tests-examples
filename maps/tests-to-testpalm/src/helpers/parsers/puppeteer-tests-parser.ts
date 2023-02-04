import BaseTestsParser, {SKIP_ALL_PATTERN} from './base-tests-parser';
import {TestsGlobal, TestCallback, SuiteCallback, SuiteFunction, TestFunction} from '../../types';

class PuppeteerTestsParser extends BaseTestsParser {
    protected _prepareGlobal(testGlobal: TestsGlobal): void {
        super._prepareGlobal(testGlobal);

        testGlobal.describe = this._getPuppeteerDescribeStub();

        testGlobal.it = this._getPuppeteerTestStub();
        testGlobal.test = this._getPuppeteerTestStub();
    }

    private _getPuppeteerDescribeStub(): SuiteFunction {
        const describeStub = (title: string, callback: SuiteCallback) => this._describeStub(title, callback);
        describeStub.skip = (title: string, callback: SuiteCallback) => {
            this._skipNext = {
                skipIn: SKIP_ALL_PATTERN
            };

            return describeStub.call(this, title, callback);
        };

        describeStub.only = describeStub.bind(this);

        return describeStub;
    }

    private _getPuppeteerTestStub(): TestFunction {
        const itStub = (title: string, callback: TestCallback) => this._itStub(title, callback);
        itStub.skip = (title: string, callback: TestCallback) => {
            this._skipNext = {
                skipIn: SKIP_ALL_PATTERN
            };

            return itStub.call(this, title, callback);
        };

        itStub.only = itStub.bind(this);

        return itStub;
    }
}

export default PuppeteerTestsParser;
