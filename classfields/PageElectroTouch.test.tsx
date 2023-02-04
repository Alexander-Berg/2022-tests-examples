jest.mock('auto-core/react/components/common/PageElectro/useElectro', () => jest.fn());
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

import React from 'react';
import { fireEvent, render, act } from '@testing-library/react';
import { useDispatch, useSelector } from 'react-redux';

import promoElectroMock from 'autoru-frontend/mockData/bunker/promo/electro';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import geo from 'auto-core/react/dataDomain/geo/mocks/geo.mock';
import useElectro from 'auto-core/react/components/common/PageElectro/useElectro';
import jurnalArticlesMock from 'auto-core/react/dataDomain/journalArticles/mocks/defaultState.mock';

import PageElectroTouch from './PageElectroTouch';

import '@testing-library/jest-dom';

const ContextProvider = createContextProvider(contextMock);
const useElectroMock = useElectro as jest.MockedFunction<typeof useElectro>;

beforeEach(() => {
    const store = mockStore({
        journalArticles: jurnalArticlesMock,
    });

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    useElectroMock.mockReturnValue({
        uchebnikArticlesStat: {
            getArticleLinkProps: jest.fn(),
            getClickMetrika: jest.fn(),
            getTagLink: jest.fn(),
        },
        hasMorePopularModels: false,
        onFetchMoreClick: () => {},
        popularModels: [],
        isPopularModelsFetching: false,
        electroBunker: promoElectroMock,
        offers: [],
        offersCount: 0,
        reviews: [],
        searchParameters: {},
        isWebview: true,
        region: geo.gidsInfo[0],
        listingRequestId: '',
        searchId: '',
    });
});

it('не должен отрендерить блок с кросслинками на главной раздела если страница открыта в вебвью', () => {
    render(
        <ContextProvider>
            <PageElectroTouch/>
        </ContextProvider>,
    );

    const component = document.querySelector('.MarkCrosslinksTouch');

    expect(component).toBeNull();
});

it('при саспенде видео должен сфолбэчится на картинку', () => {
    render(
        <ContextProvider>
            <PageElectroTouch/>
        </ContextProvider>,
    );

    act(() => {
        fireEvent.suspend(document.querySelector('.PageElectroTouch__car')!);
    });

    expect(document.querySelector('.PageElectroTouch__carWrapper_fallback')).not.toBeNull();
});
