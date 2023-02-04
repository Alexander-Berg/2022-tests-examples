const getFilterCategory = require('realty-router/lib/filters/getFilterCategory');

describe('getFilterCategory', () => {
    it('should return commecial category for commercialType param', () => {
        const params = {
            commercialType: 'OFFICE'
        };
        const category = getFilterCategory('SELL', params);

        expect(category).toEqual(params.commercialType);
    });

    it('should return category from pageType for villages and newbuildings', () => {
        expect(getFilterCategory('VILLAGES', {})).toEqual('VILLAGES');
        expect(getFilterCategory('NEWBUILDINGS', {})).toEqual('NEWBUILDINGS');
    });

    it('should return townhouse category', () => {
        const params = {
            category: 'HOUSE',
            houseType: 'TOWNHOUSE'
        };
        const category = getFilterCategory('SELL', params);

        expect(category).toEqual(params.houseType);
    });

    it('should return house category', () => {
        const params = {
            category: 'HOUSE'
        };
        const category = getFilterCategory('SELL', params);

        expect(category).toEqual('HOUSE');
    });

    it('should return default APARTMENT category', () => {
        expect(getFilterCategory('SELL', {})).toEqual('APARTMENT');
    });
});
