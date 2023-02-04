/**
 * @jest-environment jsdom
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import type { PaidServicePrice } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import { ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import CardVinReportActionButtonMobile from './CardVinReportActionButtonMobile';
import type { Props } from './CardVinReportActionButtonMobile';

const state = {
    card: cardMock,
    user: userWithAuthMock,
    config: configStateMock.value(),
};

const defaultProps: Partial<Props> = {
    vinReport: cardVinReportFree,
    contextBlock: ContextBlock.BLOCK_HISTORY,
    needPay: true,
    name: '',
    servicePrice: cardVinReportFree.billing!.service_prices[2],
    vinReportPaymentParams: { offerId: '123' },
};

const renderComponent = (props: Partial<Props>, customStore?: any) => {
    const fullProps = { ...defaultProps, ...props } as Props;

    return shallow(
        <CardVinReportActionButtonMobile { ...fullProps }/>,
        { context: { ...contextMock, store: customStore || mockStore(state) } },
    ).dive();
};

it('после клика попытается показать модал с оплатой', () => {
    const store = mockStore(state);
    const wrapper = renderComponent({}, store);

    wrapper.find('.CardVinReportActionButtonMobile').simulate('click');

    expect(store.getActions()).toEqual([
        {
            type: 'TRY_TO_BUY_REPORT',
        },
        {
            type: 'OPEN_PAYMENT_MODAL',
            payload: {
                category: 'cars',
                forceShowBundleSelector: undefined,
                from: 'api_m_default',
                offerId: '123',
                platform: 'PLATFORM_MOBILE',
                product: [
                    {
                        count: 1,
                        name: 'offers-history-reports',
                    },
                ],
                purchaseCount: 1,
                section: 'used',
                shouldUpdateUserOffersAfter: undefined,
                successText: false,
                updateOffer: undefined,
                vin_or_license_plate: undefined,
            },
        },
    ]);
});

describe('скидки и выгода', () => {
    it('выгода', () => {
        const noDiscountsVinReport = _.cloneDeep(cardVinReportFree);
        const prices = noDiscountsVinReport.billing?.service_prices.map((servicePrice) => {
            const { original_price: ogPrice, ...rest } = servicePrice;
            return rest;
        });
        noDiscountsVinReport.billing = {
            ...noDiscountsVinReport.billing,
            service_prices: prices as Array<PaidServicePrice>,
            quota_left: 0,
        };
        const wrapper = renderComponent({
            vinReport: noDiscountsVinReport,
            servicePrice: noDiscountsVinReport.billing?.service_prices[2] as PaidServicePrice,
        });
        const label = wrapper.find('.CardVinReportActionButtonMobile__discountLabel');

        expect(label.text()).toBe('Выгода 60%');
    });

    it('скидка', () => {
        const wrapper = renderComponent({});
        const label = wrapper.find('.CardVinReportActionButtonMobile__discountLabel');

        expect(label.text()).toBe('Скидка 33%');
    });

    it('ни скидок, ни выгоды', () => {
        const noDiscountsVinReport = _.cloneDeep(cardVinReportFree);
        const prices = noDiscountsVinReport.billing?.service_prices.map((servicePrice, index) => {
            return {
                ...servicePrice,
                price: (index + 1) * 100,
                original_price: (index + 1) * 100,
            };
        });
        noDiscountsVinReport.billing = {
            ...noDiscountsVinReport.billing,
            service_prices: prices as Array<PaidServicePrice>,
            quota_left: 0,
        };
        const wrapper = renderComponent({
            vinReport: noDiscountsVinReport,
            servicePrice: noDiscountsVinReport.billing?.service_prices[2] as PaidServicePrice,
        });
        const label = wrapper.find('.CardVinReportActionButtonMobile__discountLabel');

        expect(label).not.toExist();
    });
});
