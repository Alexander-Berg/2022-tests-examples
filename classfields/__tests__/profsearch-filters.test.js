const { getProfsearchBackendParamsFromFilters } = require('../profsearch-filters');

describe('getProfsearchBackendParamsFromFilters', () => {
    it('returns default category and sort', () => {
        const { category, sort } = getProfsearchBackendParamsFromFilters({});

        expect(category).toBe('APARTMENT');
        expect(sort).toBe('RELEVANCE');
    });

    it('returns passed category as is', () => {
        const { category } = getProfsearchBackendParamsFromFilters({ category: 'HOUSE' });

        expect(category).toBe('HOUSE');
    });

    it('returns the LIVINIG_SPACE as a sort option if the category is "ROOMS"', () => {
        const { sort } = getProfsearchBackendParamsFromFilters({ category: 'ROOMS', direction: 'ASC', sort: 'area' });

        expect(sort).toBe('LIVING_SPACE');
    });

    it('returns PRICE as a sort option for rooms', () => {
        const { sort } = getProfsearchBackendParamsFromFilters({ category: 'ROOMS', direction: 'ASC', sort: 'price' });

        expect(sort).toBe('PRICE');
    });

    it('returns the AREA as a sort option if the category is not "ROOMS"', () => {
        const { sort } = getProfsearchBackendParamsFromFilters({
            category: 'APARTMENT',
            direction: 'ASC',
            sort: 'area'
        });

        expect(sort).toBe('AREA');
    });

    it('returns the RELEVANCE as a sort option if the "sort" parameter is not passed', () => {
        const { sort } = getProfsearchBackendParamsFromFilters({ category: 'APARTMENT', direction: 'ASC' });

        expect(sort).toBe('RELEVANCE');
    });
});
