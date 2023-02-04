jest.mock('auto-core/react/components/common/PageElectroCard/useElectroCard', () => jest.fn());
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

import React from 'react';
import { render } from '@testing-library/react';
import { useDispatch, useSelector } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import useElectroCard from 'auto-core/react/components/common/PageElectroCard/useElectroCard';
import jurnalArticlesMock from 'auto-core/react/dataDomain/journalArticles/mocks/defaultState.mock';
import catalogTechParamMock from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import PageElectroCardTouch from './PageElectroCardTouch';

import '@testing-library/jest-dom';

const ContextProvider = createContextProvider(contextMock);
const useElectroCardMock = useElectroCard as jest.MockedFunction<typeof useElectroCard>;

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

    useElectroCardMock.mockReturnValue({
        offers: [],
        offersCount: 0,
        searchParameters: {},
        description: '',
        techParam: catalogTechParamMock.data?.entities[0].tech_param,
        vehicleName: '',
        isGarageListingPending: false,
        markInfo: catalogTechParamMock.data?.entities[0].mark_info,
        modelInfo: catalogTechParamMock.data?.entities[0].model_info,
        onGarageClick: () => {},
        isModelInGarage: false,
        mainImageSrcSet: '',
        onRemoveFromCompareClick: () => false,
        isWebview: true,
        catalogCardId: '',
        listingRequestId: '',
        searchId: '',
    });
});

it('не должен отрендерить блок с кросслинками в карточке модели если страница открыта в вебвью', () => {
    render(
        <ContextProvider>
            <PageElectroCardTouch/>
        </ContextProvider>,
    );

    const component = document.querySelector('.CrossLinks');

    expect(component).toBeNull();
});
