jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen', () => {
    return jest.fn(() => () => { });
});

import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Props } from './SalesItemNewDesignStatus';
import SalesItemNewDesignStatus from './SalesItemNewDesignStatus';

const defaultProps: Props = {
    offer: offerMock,
    openPaymentModal: paymentModalOpen,
    onAutoProlongationToggle: jest.fn(),
    onOfferActivate: jest.fn(),
    onPlacementDiscountTimerFinish: jest.fn(),
    pageType: 'sales',
    hasMosRuActionWall: false,
    onMosRuConnectClick: jest.fn(),
};

it('откроет модал если юзер кликнет на кнопку активации', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.INACTIVE)
        .withCustomVas({
            service: TOfferVas.PLACEMENT,
            prolongation_forced_not_togglable: false,
            price: 1299,
        })
        .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: false })
        .value();

    const props = {
        ...defaultProps,
        offer,
    };

    const page = renderWrapper(props);

    page.find('Button').simulate('click');

    expect(paymentModalOpen).toHaveBeenCalledWith({
        category: 'cars',
        from: 'new-lk-tab',
        offerId: '1085562758-1970f439',
        platform: 'PLATFORM_DESKTOP',
        services: [
            {
                service: 'all_sale_activate',
            },
        ],
        shouldShowSuccessTextAfter: true,
        shouldUpdateOfferAfter: true,
        shouldUpdateUserOffersAfter: undefined,
        successText: 'Платёж прошёл',
    });
});

it('активирует оффер, если активация бесплатная', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.INACTIVE)
        .withCustomVas({
            service: TOfferVas.PLACEMENT,
            prolongation_forced_not_togglable: false,
            price: 0,
        })
        .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: false })
        .value();

    const onOfferActivateMock = jest.fn();

    const props = {
        ...defaultProps,
        onOfferActivate: onOfferActivateMock,
        offer,
    };

    const page = renderWrapper(props);

    page.find('Button').simulate('click');

    expect(onOfferActivateMock).toHaveBeenCalled();
});

it('активирует оффер, если активация через МосРу', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.INACTIVE)
        .withCustomVas({
            service: TOfferVas.PLACEMENT,
            prolongation_forced_not_togglable: false,
            price: 0,
        })
        .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: false })
        .value();

    const onMosRuConnectClickMock = jest.fn();

    const props = {
        ...defaultProps,
        onMosRuConnectClick: onMosRuConnectClickMock,
        offer,
        hasMosRuActionWall: true,
    };

    const page = renderWrapper(props);

    page.find('Button').simulate('click');

    expect(onMosRuConnectClickMock).toHaveBeenCalled();
});

function renderWrapper(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <SalesItemNewDesignStatus { ...props }/>
        </ContextProvider>,
    ).dive().dive();
}
