import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import { RelativeSearchPositionStatus } from 'auto-core/react/lib/offer/getRelativeSearchPositionStatus';

import RelativeSearchPositionPopup from './RelativeSearchPositionPopup';

beforeEach(() => {
    MockDate.set('2021-12-31');
});

it('откроет модал оплаты с правильными параметрами в перекупе', () => {
    const ContextProvider = createContextProvider(contextMock);
    const openPaymentModalMock = jest.fn();
    const props = {
        offer: offerMock,
        linkUrl: 'foo',
        showButton: false,
        status: RelativeSearchPositionStatus.ABOVE_THRESHOLD,
        openPaymentModal: openPaymentModalMock,
        pageType: 'reseller-sales',
    };

    const page = shallow(
        <ContextProvider>
            <RelativeSearchPositionPopup { ...props }>
                <div className="anchorClass">anchor</div>
            </RelativeSearchPositionPopup>
        </ContextProvider>,
    ).dive();

    (page.instance() as RelativeSearchPositionPopup).handleButtonClick();

    expect(openPaymentModalMock).toHaveBeenCalledWith({
        category: 'cars',
        from: 'desktop-lk-reseller-relative_position',
        offerId: '1085562758-1970f439',
        platform: 'PLATFORM_DESKTOP',
        services: [ {
            service: 'all_sale_fresh',
        } ],
        shouldShowSuccessTextAfter: true,
        shouldUpdateOfferAfter: undefined,
        shouldUpdateUserOffersAfter: undefined,
        successText: 'Опция успешно активирована',
    });
});

it('откроет модал оплаты с правильными параметрами в частнике', () => {
    const ContextProvider = createContextProvider(contextMock);
    const openPaymentModalMock = jest.fn();
    const props = {
        offer: offerMock,
        linkUrl: 'foo',
        showButton: false,
        status: RelativeSearchPositionStatus.ABOVE_THRESHOLD,
        openPaymentModal: openPaymentModalMock,
        pageType: 'sales',
    };

    const page = shallow(
        <ContextProvider>
            <RelativeSearchPositionPopup { ...props }>
                <div className="anchorClass">anchor</div>
            </RelativeSearchPositionPopup>
        </ContextProvider>,
    ).dive();

    (page.instance() as RelativeSearchPositionPopup).handleButtonClick();

    expect(openPaymentModalMock).toHaveBeenCalledWith({
        category: 'cars',
        from: 'desktop-lk-relative_position',
        offerId: '1085562758-1970f439',
        platform: 'PLATFORM_DESKTOP',
        services: [ {
            service: 'all_sale_fresh',
        } ],
        shouldShowSuccessTextAfter: true,
        shouldUpdateOfferAfter: undefined,
        shouldUpdateUserOffersAfter: undefined,
        successText: 'Опция успешно активирована',
    });
});
