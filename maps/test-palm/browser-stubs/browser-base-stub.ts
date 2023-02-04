import type {Platform} from '../types';

const getBrowserBaseStub = (platform: Platform) => ({
    isPhone: platform === 'mobile'
});

export {getBrowserBaseStub};
