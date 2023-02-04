jest.mock('www-mobile/react/components/MarketplaceHeader/MarketplaceHeader', () => () => null);
jest.mock('www-mobile/react/components/MarketplaceJournal/MarketplaceJournal', () => () => null);
jest.mock('auto-core/react/dataDomain/listing/actions/fetchMoreGroupWithRating', () => jest.fn()
    .mockReturnValue({ type: 'something' }));

import React from 'react';
import _ from 'lodash';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import compareMock from 'autoru-frontend/mockData/state/compare.mock';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import listingMock from 'autoru-frontend/mockData/state/listing';

import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import fetchMoreGroupWithRating from 'auto-core/react/dataDomain/listing/actions/fetchMoreGroupWithRating';
import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import PageMarketplaceMark from './PageMarketplaceMark';

const ContextProvider = createContextProvider(contextMock);

import '@testing-library/jest-dom';

let listing = _.cloneDeep(listingMock);

let defaultState: any;

beforeEach(() => {
    listing = _.cloneDeep(listingMock);
    defaultState = {
        listing,
        compare: compareMock,
        ads: { data: {} },
        breadcrumbsPublicApi,
        personalizedOffersFeed: {
            data: {
                offers: [
                    offerMock,
                ],
                pagination: {
                    current: 1,
                },
            },
        },
        cardGroupGallery: {
            data: {
                promo: [],
            },
        },
    };
});

it('есть кнопка загрузить если есть страницы', () => {
    listing.data.pagination = {
        total_page_count: 2,
        page: 1,
    } as unknown as TStateListing['data']['pagination'];
    renderComponent();

    expect(document.querySelector('.PageMarketplaceMark__sectionButton')).not.toBeNull();
});

it('нет кнопки загрузить если нет страниц', () => {
    renderComponent();

    expect(document.querySelector('.PageMarketplaceMark__sectionButton')).toBeNull();
});

it('отправит экшн для подгрузки моделей при клике на кнопку', () => {
    listing.data.pagination = {
        total_page_count: 2,
        page: 1,
    } as unknown as TStateListing['data']['pagination'];
    renderComponent();
    const button = document.querySelector('.PageMarketplaceMark__sectionButton') as Element;

    userEvent.click(button);

    expect(fetchMoreGroupWithRating).toHaveBeenCalledWith({
        catalog_filter: [
            {
                mark: 'VAZ',
                model: 'KALINA',
            },
        ],
        category: 'cars',
        page_size: 4,
        page: 2,
        price_to: 50000,
        section: 'all',
        sort: 'fresh_relevance_1-desc',
    });
});

it('отправит метрику по клику на тайтл', () => {
    renderComponent();
    const title = document.querySelector('.BigImageGallery__overlayContent > .Link') as Element;
    userEvent.click(title);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-mark', 'blocks', 'gallery', 'listing-link', 'click' ]);
});

function renderComponent(state = defaultState) {
    const store = mockStore(state);
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseDispatch(store);
    mockUseSelector(state);

    return render(
        <Provider store={ store }>
            <ContextProvider>
                <PageMarketplaceMark/>
            </ContextProvider>
        </Provider>,
    );
}
