import type { StateAds, UpdateServerDataAction } from 'auto-core/react/dataDomain/ads/types';

import reducer from './reducer';
import { ADS_UPDATE_SERVER_DATA } from './types';

describe('ADS_UPDATE_SERVER_DATA', () => {
    it('должен смежить direct и перезаписать settings', () => {
        const state: StateAds = {
            isFetching: false,
            data: {
                code: 'index',
                data: {
                    direct: {
                        top: {
                            direct_premium: [],
                        },
                    },
                    rtb: {
                        'top:0': {
                            meta: {
                                settings: {},
                            },
                        },
                    },
                },
                settings: {
                    top: {
                        id: 'top',
                        sources: [],
                    },
                },
                statId: '100',
            },
        };
        const action: UpdateServerDataAction = {
            type: ADS_UPDATE_SERVER_DATA,
            payload: {
                direct: {
                    r1: {
                        direct_premium: [],
                    },
                },
                settings: {
                    r1: {
                        id: 'r1',
                        sources: [],
                    },
                },
            },
        };

        expect(reducer(state, action)).toEqual({
            isFetching: false,
            data: {
                code: 'index',
                data: {
                    direct: {
                        r1: {
                            direct_premium: [],
                        },
                        top: {
                            direct_premium: [],
                        },
                    },
                    rtb: {
                        'top:0': {
                            meta: {
                                settings: {},
                            },
                        },
                    },
                },
                settings: {
                    r1: {
                        id: 'r1',
                        sources: [],
                    },
                },
                statId: '100',
            },
        });
    });
});
