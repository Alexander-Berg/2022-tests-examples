import {TestCaseAttributes} from '@yandex-int/tests-to-testpalm/out/types';
import type {GetBrowserStub} from '../types';
import {getBrowserBaseStub} from './browser-base-stub';

const getBrowserAttributesStub: GetBrowserStub<TestCaseAttributes> = (platform) => ({
    ...getBrowserBaseStub(platform)
});

export {getBrowserAttributesStub};
