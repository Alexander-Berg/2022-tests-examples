import BaseTestsRunner from './base-tests-runner';
import {TestCallback, Stub} from '../../types';

class PuppeteerTestsRunner extends BaseTestsRunner {
    protected _itCallbackRunner(itCallback: TestCallback, stub: Stub): Promise<void> {
        this._testGlobal.page = stub;
        // eslint-disable-next-line no-useless-call
        return itCallback.call(null);
    }
}

export default PuppeteerTestsRunner;
