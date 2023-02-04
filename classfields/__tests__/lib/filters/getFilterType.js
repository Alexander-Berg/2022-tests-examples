const getFilterType = require('realty-router/lib/filters/getFilterType');

describe('getFilterType', () => {
    it('should return filterType from pageType for VILLAGES and NEWBUILDINGS', () => {
        expect(getFilterType('VILLAGES', {})).toEqual('VILLAGES');
        expect(getFilterType('NEWBUILDINGS', {})).toEqual('NEWBUILDINGS');
    });

    it('should return filterType for commercial category', () => {
        const params = {
            category: 'COMMERCIAL'
        };
        const filterType = getFilterType('SELL', params);

        expect(filterType).toEqual(params.category);
    });

    it('should return filterType for commercial type', () => {
        const params = {
            commercialType: 'OFFICE'
        };
        const filterType = getFilterType('SELL', params);

        expect(filterType).toEqual('COMMERCIAL');
    });

    it('should return filterType from type param', () => {
        const params = {
            type: 'SELL'
        };
        const filterType = getFilterType('SEARCH', params);

        expect(filterType).toEqual(params.type);
    });

    it('should return filterType by default', () => {
        const filterType = getFilterType('SEARCH', {});

        expect(filterType).toEqual('SELL');
    });

    it('should return filterType by default for unavailable pageType', () => {
        const filterType = getFilterType('TEST', {});

        expect(filterType).toEqual('SELL');
    });
});
