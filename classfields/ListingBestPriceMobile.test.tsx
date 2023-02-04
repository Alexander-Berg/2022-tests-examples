import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import ListingBestPriceMobile from './ListingBestPriceMobile';

describe('отдает правильную ссылку', () => {
    it('если регион из бункера совпадает с регионом в gids', () => {
        const store = mockStore({
            matchApplication: {
                allowedMarksModels: {},
                markModelDetails: [],
                isPending: false,
                hasError: false,
            },
            bunker: { 'common/best_price_mixed_listing': [ 1 ] },
            geo: { geoParents: [], gids: [ 1 ] },
        });

        const bestPriceComponent = shallow(
            <Provider store={ store }>
                <ListingBestPriceMobile/>
            </Provider>,
            { context: contextMock },
        ).dive().dive().find('Button');

        expect(bestPriceComponent).toHaveProp('url', 'link/get-best-price/?category=cars&section=new&from=listing&blockName=mixed-listing&rid=1');
    });

    it('если регион из бункера совпадает с регионом в geoParentsIds', () => {
        const store = mockStore({
            matchApplication: {
                allowedMarksModels: {},
                markModelDetails: [],
                isPending: false,
                hasError: false,
            },
            bunker: { 'common/best_price_mixed_listing': [ 1 ] },
            geo: { geoParents: [ { id: 1 } ], gids: [] },
        });

        const bestPriceComponent = shallow(
            <Provider store={ store }>
                <ListingBestPriceMobile/>
            </Provider>,
            { context: contextMock },
        ).dive().dive().find('Button');

        expect(bestPriceComponent).toHaveProp('url', 'link/get-best-price/?category=cars&section=new&from=listing&blockName=mixed-listing&rid=1');
    });

    it('если регион из бункера не совпадает ни с каким регионом', () => {
        const store = mockStore({
            matchApplication: {
                allowedMarksModels: {},
                markModelDetails: [],
                isPending: false,
                hasError: false,
            },
            bunker: { 'common/best_price_mixed_listing': [ 1 ] },
            geo: { geoParents: [ { id: 112 } ], gids: [ 113 ] },
        });

        const bestPriceComponent = shallow(
            <Provider store={ store }>
                <ListingBestPriceMobile/>
            </Provider>,
            { context: contextMock },
        ).dive().dive().find('Button');

        expect(bestPriceComponent).toHaveProp('url', 'link/get-best-price/?category=cars&section=new&from=listing&blockName=mixed-listing');
    });

    it('если передаем isFromCardGroup', () => {
        const store = mockStore({
            matchApplication: {
                allowedMarksModels: {},
                markModelDetails: [],
                isPending: false,
                hasError: false,
            },
            bunker: { 'common/best_price_mixed_listing': [ 1 ] },
            geo: { geoParents: [], gids: [ 1 ] },
        });

        const bestPriceComponent = shallow(
            <Provider store={ store }>
                <ListingBestPriceMobile isFromCardGroup/>
            </Provider>,
            { context: contextMock },
        ).dive().dive().find('Button');

        expect(bestPriceComponent).toHaveProp('url', 'link/get-best-price/?category=cars&section=new&from=card-group&blockName=listing-new');
    });
});
