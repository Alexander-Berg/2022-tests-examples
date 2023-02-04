const check = require('./checkParamsForTrucksCount');
describe('checkParamsForTrucksCount: check incorrect params', () => {
    it('returns false on undefined', () => {
        const result = check({ params: {} });
        expect(result).toEqual(false);
    });
    it('returns false on string', () => {
        const result = check({ params: 'test' });
        expect(result).toEqual(false);
    });
    it('returns false on array', () => {
        const result = check({ params: [] });
        expect(result).toEqual(false);
    });
});
describe('checkParamsForTrucksCount: check correct params', () => {
    it('returns false on empty params', () => {
        const result = check({ params: {} });
        expect(result).toEqual(false);
    });
    it('returns false on params with only mark in nameplate', () => {
        const result = check({
            params: {
                catalog_filter: [ { mark: 'FORD' } ],
            },
        });
        expect(result).toEqual(false);
    });
    it('returns true on params with mark&model in nameplate', () => {
        const result = check({
            params: {
                catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
            },
        });
        expect(result).toEqual(true);
    });
    it('returns false on params with mark&model and extra params in nameplate', () => {
        const result = check({
            params: {
                catalog_filter: [ { mark: 'FORD', model: 'FOCUS', generation: '123' } ],
                test: 1,
            },
        });
        expect(result).toEqual(true);
    });
    it('returns true on params with mark&model in nameplate and more params', () => {
        const result = check({
            params: {
                catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
                category: 'new',
            },
        });
        expect(result).toEqual(true);
    });
});
