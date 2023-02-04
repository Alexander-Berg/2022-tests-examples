const getPostMatchFilters = require('realty-router/lib/filters/getPostMatchFilters');

describe('getPostMatchFilters', () => {
    it('should convert one filter alias to filter params', () => {
        const params = {
            someParam: 'someValue',
            filter1: 's-balkonom'
        };

        const postMatchParams = getPostMatchFilters(params);

        expect(postMatchParams).toEqual({
            someParam: 'someValue',
            balcony: 'BALCONY'
        });
    });

    it('should convert two filters aliases to filters params', () => {
        const params = {
            someParam: 'someValue',
            filter1: 's-balkonom',
            filter2: 's-remontom'
        };

        const postMatchParams = getPostMatchFilters(params);

        expect(postMatchParams).toEqual({
            someParam: 'someValue',
            balcony: 'BALCONY',
            renovation: [ 'EURO', 'COSMETIC_DONE' ]
        });
    });

    it('should remove unavailable filters aliases from params', () => {
        const params = {
            someParam: 'someValue',
            filter1: 'filter',
            filter2: 'someFilter'
        };

        const postMatchParams = getPostMatchFilters(params);

        expect(postMatchParams).toEqual({
            someParam: 'someValue'
        });
    });
});
