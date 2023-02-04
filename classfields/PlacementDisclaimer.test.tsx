import '@testing-library/jest-dom';
import React from 'react';
import { Provider } from 'react-redux';
import { render } from '@testing-library/react';

import { PaidReason } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { AppState } from 'www-poffer/react/store/AppState';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';

import PlacementDisclaimer from './PlacementDisclaimer';

let initialState: Partial<AppState>;
const offerDraft = offerDraftMock.value();

beforeEach(() => {
    initialState = {
        bunker: getBunkerMock([ 'common/vas' ]),
        config: configStateMock
            .withPageParams({ category: 'cars' })
            .value(),
        offerDraft,
    };
});

describe('не показываем', () => {
    it('если страница редактирования', () => {
        const config = configStateMock.withPageType('edit').value();

        renderContainer({ config });

        const component = document.querySelector('.PlacementDisclaimer');
        expect(component).not.toBeInTheDocument();
    });

    it('если нет услуги all_sale_activate', () => {
        const offerDraftWithoutServicePrices = offerDraftMock.withOfferMock(
            cloneOfferWithHelpers(offerMock).
                withServicePrices([]),
        ).value();

        renderContainer({ offerDraft: offerDraftWithoutServicePrices });

        const component = document.querySelector('.PlacementDisclaimer');
        expect(component).not.toBeInTheDocument();
    });

    it('если нет prolongation_forced_not_togglable в услуге', () => {
        renderContainer();

        const component = document.querySelector('.PlacementDisclaimer');
        expect(component).not.toBeInTheDocument();
    });
});

describe('показываем нужный текст', () => {
    it('если есть скидка на продление', () => {
        const servicePrices = offerDraft.data.offer.service_prices.map((service) => {
            if (service.service !== TOfferVas.PLACEMENT) {
                return service;
            }

            return {
                ...service,
                prolongation_forced_not_togglable: true,
                price: 1800,
                auto_prolong_price: 1499,
                days: 7,
                paid_reason: PaidReason.FREE_LIMIT,
            };
        });

        const offerDraftWithServicePrices = offerDraftMock.withOfferMock(
            cloneOfferWithHelpers(offerMock).
                withServicePrices(servicePrices),
        ).value();

        renderContainer({ offerDraft: offerDraftWithServicePrices });

        const component = document.querySelector('.PlacementDisclaimer__discount');
        expect(component).toBeInTheDocument();
    });

    it('если перепродажа', () => {
        const servicePrices = offerDraft.data.offer.service_prices.map((service) => {
            if (service.service !== TOfferVas.PLACEMENT) {
                return service;
            }

            return {
                ...service,
                prolongation_forced_not_togglable: true,
                auto_prolong_price: 4000000,
                days: 7,
                paid_reason: PaidReason.PAYMENT_GROUP,
            };
        });

        const offerDraftWithServicePrices = offerDraftMock.withOfferMock(
            cloneOfferWithHelpers(offerMock).
                withServicePrices(servicePrices),
        ).value();

        renderContainer({ offerDraft: offerDraftWithServicePrices });

        const component = document.querySelector('.PlacementDisclaimer__paidOffer');
        expect(component).toBeInTheDocument();
    });
});

function renderContainer(propsState = {}) {
    const { mockUseSelector } = applyUseSelectorMock();
    const ContextProvider = createContextProvider(contextMock);
    const state = { ...initialState, ...propsState };
    const store = mockStore(state);
    mockUseSelector(state);

    return render(
        <ContextProvider>
            <Provider store={ store }>
                <PlacementDisclaimer/>
            </Provider>
        </ContextProvider>,
    );
}
