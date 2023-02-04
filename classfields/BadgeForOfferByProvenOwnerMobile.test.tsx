/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import BadgeForOfferByProvenOwnerMobile from './BadgeForOfferByProvenOwnerMobile';

const Context = createContextProvider(contextMock);

const renderComponent = (storeMock: Record<string, any>, offer: Offer) => {
    const store = mockStore(storeMock);

    return shallow(
        <Context>
            <Provider store={ store }>
                <BadgeForOfferByProvenOwnerMobile offer={ offer }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
};

it('должен нарисовать кнопку аутентификации для незалогина', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'proven_owner' ])
        .value();

    const tree = renderComponent({ user: { data: {} } }, offer);

    expect(tree.find('Connect(AuthButton)')).toHaveLength(1);
});

it('должен нарисовать простую кнопку для залогина', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'proven_owner' ])
        .value();
    const store = {
        user: {
            data: { auth: true },
        },
    };

    const tree = renderComponent(store, offer);

    expect(tree.find('Button.BadgeForOfferByProvenOwnerMobile__link')).toHaveLength(1);
});

it('должен нарисовать бейдж для оффера от собственника', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'proven_owner' ])
        .value();

    const tree = renderComponent({ user: { data: {} } }, offer);

    expect(tree.find('Badge')).toExist();
});

it('не должен нарисовать бейдж для оффера, если собственник не проверен', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([])
        .value();

    const tree = renderComponent({ user: { data: {} } }, offer);

    expect(tree).toBeEmptyRender();
});

it('не должен нарисовать бейдж для оффера, если авто забронировано', async() => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .value();

    const tree = renderComponent({ user: { data: {} } }, offer);

    expect(tree).toBeEmptyRender();
});
