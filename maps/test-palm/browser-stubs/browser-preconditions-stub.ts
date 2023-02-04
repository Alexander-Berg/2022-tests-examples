import {getBrowserBaseStub} from './browser-base-stub';
import type {GetBrowserStub} from '../types';

type AdditionalStub = {
    template: string;
};

const getBrowserPreconditionsStub: GetBrowserStub<Record<string, string>, AdditionalStub> = (platform) => ({
    ...getBrowserBaseStub(platform),
    template: ''
});

export {getBrowserPreconditionsStub};
