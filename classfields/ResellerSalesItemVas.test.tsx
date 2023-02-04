jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen', () => {
    return jest.fn(() => () => { });
});

import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { TSalesServiceInfo } from 'auto-core/react/lib/offer/getServicePricesForPrivateAndReseller';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import getServicePricesForPrivateAndReseller from 'auto-core/react/lib/offer/getServicePricesForPrivateAndReseller';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Props } from './ResellerSalesItemVas';
import ResellerSalesItemVas from './ResellerSalesItemVas';

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;

const initialProps = {
    onAutoProlongationToggle: jest.fn(),
    onAutoRenewChange: jest.fn(),
    pageType: 'reseller-sales',
    onOfferLoadStats: jest.fn(),
    hasTiedCards: true,
};

it('откроет модал если юзер кликнет на ВАС', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCustomVas({ service: TOfferVas.VIP, price: 9999 })
        .withActiveVas([])
        .value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.VIP) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        openPaymentModal: paymentModalOpenMock,
        service,
        isVipActive: false,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock).toHaveBeenCalledWith({
        category: 'cars',
        from: 'desktop-lk-reseller',
        offerId: '1085562758-1970f439',
        platform: 'PLATFORM_DESKTOP',
        services: [
            {
                service: 'package_vip',
            },
        ],
        shouldShowSuccessTextAfter: true,
        shouldUpdateOfferAfter: true,
        shouldUpdateUserOffersAfter: false,
        successText: 'Опция успешно активирована',
    });
});

it('не будет открывать модал, если сервис Поднятие и ВИП активен', () => {
    const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.FRESH) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        openPaymentModal: paymentModalOpenMock,
        service,
        isVipActive: true,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock).not.toHaveBeenCalled();
});

it('не будет открывать модал, если сервис уже активен', () => {
    const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.TURBO ]).value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.TURBO) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        openPaymentModal: paymentModalOpenMock,
        service,
        isVipActive: false,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock).not.toHaveBeenCalled();
});

it('не будет открывать модал, если сервис задизейблен', () => {
    const offer = cloneOfferWithHelpers(offerMock).withActiveVas([ TOfferVas.VIP ]).value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.TURBO) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        openPaymentModal: paymentModalOpenMock,
        service,
        isVipActive: false,
        hasTiedCards: true,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock).not.toHaveBeenCalled();
});

it('откроет модал с параметрами shouldUpdateOfferAfter=false и shouldUpdateUserOffersAfter=true, если есть скидка на ВАС', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withActiveVas([])
        .value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.VIP) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        service,
        isVipActive: true,
        openPaymentModal: paymentModalOpenMock,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateOfferAfter).toEqual(false);
    expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateUserOffersAfter).toEqual(true);
});

it('откроет модал с параметрами shouldUpdateOfferAfter=true и shouldUpdateUserOffersAfter=false, если нет скидки на ВАС', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withCustomVas({ service: TOfferVas.VIP, price: 999999 })
        .withActiveVas([])
        .value();
    const service = getServicePricesForPrivateAndReseller(offer).find(service => service.service === TOfferVas.VIP) as TSalesServiceInfo;

    const page = renderShallowWrapper({
        ...initialProps,
        offer,
        service,
        isVipActive: true,
        openPaymentModal: paymentModalOpenMock,
    });

    page.find('.ResellerSalesItemVas').simulate('click');

    expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateOfferAfter).toEqual(true);
    expect(paymentModalOpenMock.mock.calls[0][0].shouldUpdateUserOffersAfter).toEqual(false);
});

function renderShallowWrapper(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <ResellerSalesItemVas
                { ...props }
            />
        </ContextProvider>,
    ).dive().dive();
}
