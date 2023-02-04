import type { SearchInstance } from '@vertis/schema-registry/ts-types-snake/auto/api/searches_model';

import getSearchParams from './getSearchParams';

const catalogFilter = [ {
    transmission: [ 'MECHANICAL' ],
} ];

const carsParams = {
    transmission: [ 'MECHANICAL' ],
};

describe('проверяем отказоустойчивость (не падает от кривых данных)', () => {
    it('нет params', () => {
        expect(getSearchParams({} as unknown as SearchInstance)).toBe(undefined);
    });

    it('нет cars_params', () => {
        expect(getSearchParams({
            params: {
                catalog_filter: catalogFilter,
            },
        } as unknown as SearchInstance)).toEqual({
            catalog_filter: catalogFilter,
        });
    });

    it('все есть', () => {
        expect(getSearchParams({
            params: {
                catalog_filter: catalogFilter,
                cars_params: carsParams,
            },
        } as unknown as SearchInstance)).toEqual({
            catalog_filter: catalogFilter,
            transmission: [ 'MECHANICAL' ],
        });
    });
});
