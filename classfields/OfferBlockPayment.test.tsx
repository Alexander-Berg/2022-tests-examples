import '@testing-library/jest-dom';
import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';
import _ from 'lodash';
import userEvent from '@testing-library/user-event';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { AppState } from 'www-poffer/react/store/AppState';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';

import OfferBlockPayment from './OfferBlockPayment';

let initialState: Partial<AppState>;
const offerDraft = offerDraftMock.value();
const card = offerMock;

beforeEach(() => {
    initialState = {
        bunker: getBunkerMock([ 'common/vas' ]),
        config: configStateMock
            .withPageParams({ category: 'cars' })
            .value(),
        offerDraft,
        card,
    };
});

const onClick = jest.fn();

describe('показываем кнопку сохранить', () => {
    it('если нет услуги all_sales_activate', () => {
        const offerDraftWithoutServicePrices = offerDraftMock.withOfferMock(
            cloneOfferWithHelpers(offerMock).
                withServicePrices([]),
        ).value();

        const state = {
            ..._.cloneDeep(initialState),
            offerDraft: offerDraftWithoutServicePrices,
        } as unknown as Partial<AppState>;

        const { queryByText } = renderContainer({ state });
        const btn = queryByText('Разместить бесплатно на 60 дней');
        const freeSaveBtn = queryByText('Сохранить') as HTMLElement;
        expect(freeSaveBtn).not.toBe(null);
        expect(btn).toBe(null);
        userEvent.click(freeSaveBtn);
        expect(onClick).toHaveBeenCalledTimes(1);
        expect(onClick.mock.calls[0][0]).toHaveLength(0);
    });

    it('если мы на странице редактирования', () => {
        const state = _.cloneDeep(initialState);
        state.config = configStateMock.withPageParams({ category: 'cars', form_type: 'edit' }).value();

        const { queryByText } = renderContainer({ state });
        const btn = queryByText('Сохранить') as HTMLElement;
        const paidBlock = document.querySelector('.OfferBlockPaymentCard');

        expect(btn).not.toBe(null);
        expect(paidBlock).toBe(null);
        userEvent.click(btn);
        expect(onClick).toHaveBeenCalledTimes(1);
        expect(onClick.mock.calls[0][0]).toHaveLength(0);
    });
});

it('если услуга бесплатна, то показываем только кнопку разместить бесплатно', () => {
    const servicePrices = offerDraft.data.offer.service_prices.map((service) => {
        if (service.service !== TOfferVas.PLACEMENT) {
            return service;
        }

        return {
            ...service,
            original_price: 0,
            price: 0,
        };
    });

    const offerDraftWithServicePrices = offerDraftMock.withOfferMock(
        cloneOfferWithHelpers(offerMock).
            withServicePrices(servicePrices),
    ).value();
    const state = {
        ..._.cloneDeep(initialState),
        offerDraft: offerDraftWithServicePrices,
    } as unknown as Partial<AppState>;

    const { queryByText } = renderContainer({ state });
    const btn = queryByText('Разместить бесплатно на 60 дней') as HTMLElement;
    const saveButton = queryByText('Сохранить');
    const paidBlock = document.querySelector('.OfferBlockPaymentCard');

    userEvent.click(btn);

    expect(btn).not.toBe(null);
    expect(saveButton).toBe(null);
    expect(paidBlock).toBe(null);

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onClick.mock.calls[0][0]).toHaveLength(0);
});

it('если услуга платна, то показываем блок с оплатой', () => {
    const servicePrices = offerDraft.data.offer.service_prices.map((service) => {
        if (service.service !== TOfferVas.PLACEMENT) {
            return service;
        }

        return {
            ...service,
            original_price: 1400,
            price: 1499,
        };
    });
    const offerDraftWithServicePrices = offerDraftMock.withOfferMock(
        cloneOfferWithHelpers(offerMock).
            withServicePrices(servicePrices),
    ).value();
    const state = {
        ..._.cloneDeep(initialState),
        offerDraft: offerDraftWithServicePrices,
    } as unknown as Partial<AppState>;
    const { queryByText } = renderContainer({ state });
    const saveButton = queryByText('Сохранить');
    const paidBlock = document.querySelector('.OfferBlockPaymentCard');
    const payButton = queryByText(/разместить/i) as HTMLElement;

    expect(saveButton).toBe(null);
    expect(payButton).not.toBe(null);
    expect(paidBlock).not.toBe(null);

    userEvent.click(payButton);

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(onClick.mock.calls[0][0]).toHaveLength(1);

});

function renderContainer({ state = initialState, props = {} }) {
    const { mockUseSelector } = applyUseSelectorMock();
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(state);
    mockUseSelector(state);

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <OfferBlockPayment
                    onClick={ onClick }
                    isPending={ false }
                    { ...props }
                />
            </Provider>
        </ContextProvider>,
    );
}
