import { ICoreStore } from 'realty-core/view/react/common/reducers/types';

import { getShowOutdatedValue } from '../../convert-showoutdated-params';

const defaultParams = {
    rgid: '741964',
    type: 'SELL',
} as Exclude<ICoreStore['page']['params'], null | undefined>;

describe('getShowOutdatedValue', () => {
    it('site-search, value undefined', () => {
        const showOutdated = getShowOutdatedValue(defaultParams, 'sites-search');
        expect(showOutdated).toEqual('YES');
    });

    it('site-search-map, value undefined', () => {
        const showOutdated = getShowOutdatedValue(defaultParams, 'sites-search-map');
        expect(showOutdated).toEqual('NO');
    });

    it('site-search, value YES', () => {
        const showOutdated = getShowOutdatedValue(
            Object.assign(defaultParams, { showOutdated: 'YES' }),
            'sites-search'
        );
        expect(showOutdated).toEqual('YES');
    });

    it('site-search-map, value YES', () => {
        const showOutdated = getShowOutdatedValue(
            Object.assign(defaultParams, { showOutdated: 'YES' }),
            'sites-search-map'
        );
        expect(showOutdated).toEqual('YES');
    });

    it('site-search, value NO', () => {
        const showOutdated = getShowOutdatedValue(Object.assign(defaultParams, { showOutdated: 'NO' }), 'sites-search');
        expect(showOutdated).toEqual('NO');
    });

    it('site-search-map, value NO', () => {
        const showOutdated = getShowOutdatedValue(
            Object.assign(defaultParams, { showOutdated: 'NO' }),
            'sites-search-map'
        );
        expect(showOutdated).toEqual('NO');
    });
});
