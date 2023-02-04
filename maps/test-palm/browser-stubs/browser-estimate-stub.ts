import {getBrowserBaseStub} from './browser-base-stub';
import type {GetBrowserStub} from '../types';

const getBrowserEstimateStub: GetBrowserStub<number> = (platform) => ({
    ...getBrowserBaseStub(platform)
});

export {getBrowserEstimateStub};
