const buildFilters = require('realty-router/lib/filters/buildFilters');

describe('buildFilters', () => {
    it('should match one filter by one param', () => {
        const params = {
            balcony: 'BALCONY'
        };

        const resultParams = buildFilters('SEARCH', params);

        expect(resultParams).toEqual([ 's-balkonom' ]);
    });

    it('should match 1 filter by one param with multiple values', () => {
        const params = {
            renovation: [ 'EURO', 'COSMETIC_DONE' ]
        };

        const resultParams = buildFilters('SEARCH', params);

        expect(resultParams).toEqual([ 's-remontom' ]);
    });

    it('should match one filter by many params', () => {
        const params = {
            metroTransport: 'ON_FOOT',
            timeToMetro: '10'
        };

        const resultParams = buildFilters('SEARCH', params);

        expect(resultParams).toEqual([ 'ryadom-metro' ]);
    });

    it('should match two filters by many params', () => {
        const params = {
            balcony: 'BALCONY',
            apartments: 'YES'
        };

        const resultParams = buildFilters('SEARCH', params);

        expect(resultParams).toEqual([ 'apartamenty', 's-balkonom' ]);
    });

    it('should not match filters with excess params count', () => {
        const params = {
            balcony: 'BALCONY',
            apartments: 'YES',
            metroTransport: 'ON_FOOT',
            timeToMetro: '10'
        };

        const resultParams = buildFilters('SEARCH', params);

        expect(resultParams).toEqual([ ]);
    });
});
