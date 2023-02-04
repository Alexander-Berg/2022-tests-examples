const getPreBuildFilters = require('realty-router/lib/filters/getPreBuildFilters');

describe('getPreBuildFilters', () => {
    it('should ignore params without filters matching', () => {
        const pageType = 'SEARCH';
        const params = {
            someParam: 'someValue'
        };
        const resultParams = getPreBuildFilters(pageType, params);

        expect(resultParams).toEqual(params);
    });

    it('should build one filter and remove matched params', () => {
        const pageType = 'SEARCH';
        const params = {
            someParam: 'someValue',
            balcony: 'BALCONY'
        };
        const resultParams = getPreBuildFilters(pageType, params);

        expect(resultParams).toEqual({
            someParam: 'someValue',
            filter1: 's-balkonom'
        });
    });

    it('should build two filters and remove matched params', () => {
        const pageType = 'SEARCH';
        const params = {
            someParam: 'someValue',
            balcony: 'BALCONY',
            renovation: [ 'EURO', 'COSMETIC_DONE' ]
        };
        const resultParams = getPreBuildFilters(pageType, params);

        expect(resultParams).toEqual({
            someParam: 'someValue',
            filter1: 's-remontom',
            filter2: 's-balkonom'
        });
    });
});
