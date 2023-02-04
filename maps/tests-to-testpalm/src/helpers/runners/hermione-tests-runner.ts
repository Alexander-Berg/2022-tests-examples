import BaseTestsRunner from './base-tests-runner';
import {TestCallback, Stub} from '../../types';

class HermioneTestsRunner extends BaseTestsRunner {
    protected _itCallbackRunner(itCallback: TestCallback, stub: Stub): Promise<void> {
        return itCallback.call({browser: stub});
    }
}

export default HermioneTestsRunner;
