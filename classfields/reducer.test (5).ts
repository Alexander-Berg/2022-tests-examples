import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import ActionTypes from 'auto-core/react/dataDomain/catalogListing/actionTypes';
import type { CatalogListingAction } from 'auto-core/react/dataDomain/catalogListing/types';

import type { TOfferListing } from 'auto-core/types/TOfferListing';

import reducer from './reducer';

it('должен положить данные в стор при загрузке страницы', () => {
    const state = {
        isPending: false,
        isError: false,
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            catalogListing: {
                offers: [ {}, {} ],
            } as unknown as TOfferListing,
        },

    } as CatalogListingAction;

    const result = reducer(state, action);

    expect(result.data?.offers).toHaveLength(2);
});

it('должен подложить данные в стор при CATALOG_LISTING_MORE_RESOLVED', () => {
    const state = {
        isPending: false,
        isError: false,
        data: {
            offers: [ {}, {} ],
        } as unknown as TOfferListing,
    };

    const action = {
        type: ActionTypes.CATALOG_LISTING_MORE_RESOLVED,
        payload: {
            data: {
                offers: [ {}, {} ],
            } as unknown as TOfferListing,
        },

    } as CatalogListingAction;

    const result = reducer(state, action);

    expect(result.data?.offers).toHaveLength(4);
});

it('должен установить isPending в true при CATALOG_LISTING_FETCHING', () => {
    const action = {
        type: ActionTypes.CATALOG_LISTING_FETCHING,
    } as CatalogListingAction;

    const result = reducer(undefined, action);

    expect(result.isPending).toEqual(true);
});

it('должен установить isError в true при CATALOG_LISTING_REJECTED', () => {
    const action = {
        type: ActionTypes.CATALOG_LISTING_REJECTED,
    } as CatalogListingAction;

    const result = reducer(undefined, action);

    expect(result.isError).toEqual(true);
});
