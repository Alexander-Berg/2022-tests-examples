import { getQueryByFiltersState, getFiltersStateByQuery } from '../errors-filters-query';
import { initialFilterValues } from 'view/store/stores/client-errors-store/filters-store';

describe('clients filters query', () => {
    it('should convert initial filters state to query and back', () => {
        const filters = {
            ...initialFilterValues
        };

        return expect(getFiltersStateByQuery(getQueryByFiltersState(filters))).toEqual(filters);
    });

    it('should convert non-default filters state to query and back', () => {
        const filters = {
            segments: [ 'new_building' ],
            regionIds: [ '165705', '587795' ],
            types: [ 'no_plan_photo' ],
            period: {
                from: '12.02.13',
                to: undefined
            },
            sources: [ 'xml' ],
            feedIds: [ 'feedId' ],
            offerIds: [ 'offerId' ],
            sort: { order: 'type', direction: 'asc' },
            pager: { pageNum: 2, pageSize: 10 }
        };

        return expect(getFiltersStateByQuery(getQueryByFiltersState(filters))).toEqual(filters);
    });

    it('should map non-default filters to query', () => {
        const filters = {
            segments: [ 'new_building' ],
            regionIds: [ '165705', '587795' ],
            types: [ 'no_plan_photo', 'some_another_error' ],
            period: {
                from: '12.03.1923',
                to: '14.03.1923'
            },
            sources: [ 'xml' ],
            feedIds: [ 'feedId', 'feedId2' ],
            offerIds: [ 'offerId', 'offerId2' ],
            sort: { order: 'type', direction: 'desc' },
            pager: { pageNum: 2, pageSize: 10 }
        };

        const query = getQueryByFiltersState(filters);

        return expect(query).toEqual({
            segments: 'new_building',
            regions: '165705,587795',
            types: 'no_plan_photo,some_another_error',
            period: '12.03.1923-14.03.1923',
            feeds: 'feedId,feedId2',
            offers: 'offerId,offerId2',
            sources: 'xml',
            sort_order: 'type',
            sort_direction: 'desc',
            page_size: '10',
            page_num: '2'
        });
    });

    it('should map default filters to query', () => {
        const filters = {
            ...initialFilterValues
        };

        const query = getQueryByFiltersState(filters);

        return expect(query).toEqual({
            segments: undefined,
            regions: undefined,
            types: undefined,
            period: undefined,
            feeds: undefined,
            offers: undefined,
            sources: undefined,
            sort_order: undefined,
            sort_direction: undefined,
            page_size: undefined,
            page_num: undefined
        });
    });
});
