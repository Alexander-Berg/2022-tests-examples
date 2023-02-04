jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

jest.mock('auto-core/react/dataDomain/catalogListing/actions/fetchMore', () => jest.fn());

import _ from 'lodash';
import { useDispatch, useSelector } from 'react-redux';
import { renderHook, act } from '@testing-library/react-hooks';

import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import listing from 'autoru-frontend/mockData/state/listing';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import { journalArticleMock, journalArticlesMock } from 'auto-core/react/dataDomain/journalArticles/mocks';
import { Category } from 'auto-core/react/dataDomain/journalArticles/types';
import breadcrumbsPublicApi from 'auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock';
import catalogListing from 'auto-core/react/dataDomain/catalogListing/mocks/catalogListing.mock';
import geo from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import useElectro from 'auto-core/react/components/common/PageElectro/useElectro';
import fetchMore from 'auto-core/react/dataDomain/catalogListing/actions/fetchMore';

import type TContext from 'auto-core/types/TContext';

import reviews from 'www-desktop/react/components/Index/IndexReviews/mocks/reviews.mock';

const defaultState = {
    bunker: getBunkerMock([ 'promo/electro' ]),
    journalArticles: journalArticlesMock.withArticles(
        _.times(10, () => journalArticleMock.withCategory(Category.MAIN).value()),
    ).value(),
    reviews,
    listing,
    breadcrumbsPublicApi,
    catalogListing,
    geo,
};

describe('популярные модели', () => {

    describe('hasMorePopularModels', () => {

        it('false, если больше нет страниц', () => {
            mockRedux();
            const { result } = render();
            expect(result.current.hasMorePopularModels).toBe(false);
        });

        it('true, если есть ещё страницы', () => {
            const newState = _.cloneDeep(defaultState);
            newState.catalogListing.data.pagination.total_page_count = 2;
            newState.catalogListing.data.pagination.page = 1;
            mockRedux(newState);
            const { result } = render();
            expect(result.current.hasMorePopularModels).toBe(true);
        });

    });

    describe('onFetchMoreClick', () => {

        it('если больше нет страниц - экшн не вызовется', () => {
            mockRedux();
            const { result } = render();
            act(async() => {
                await result.current.onFetchMoreClick();
            });
            expect(fetchMore).toHaveBeenCalledTimes(0);
        });

        it('вызовет экшн для обновления стора', () => {
            const newState = _.cloneDeep(defaultState);
            newState.catalogListing.data.pagination.total_page_count = 2;
            newState.catalogListing.data.pagination.page = 1;
            mockRedux(newState);
            const { result } = render();
            act(async() => {
                await result.current.onFetchMoreClick();
            });
            expect(fetchMore).toHaveBeenCalledTimes(1);
        });

    });

});

function render() {
    return renderHook(() => useElectro(contextMock as unknown as TContext));
}

function mockRedux(state = defaultState) {
    const store = mockStore(state);

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );
}
