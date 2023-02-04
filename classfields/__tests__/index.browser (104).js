import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { AlfabankPromoMortgage } from '../';

const props = {
    geo: {
        id: 1,
        rgid: 741964,
        type: 'SUBJECT_FEDERATION'
    }
};

const initialState = {
    user: {
        id: 1
    },
    alfaBankMortgage: {
        params: {
            rateBaseSecondary: 0.0879,
            rateDiscountYandex: 0.004,
            rateDiscountYandexSecondary: 0.007,
            rateDiscountBank: 0.001,
            rateSupportMax: 0.0619,
            rateSupportMin: 0.0599,
            regionalParams: [
                {
                    geoId: 1,
                    sumTransit: 6000000,
                    sumSupportMax: 12000000
                },
                {
                    geoId: 10174,
                    sumTransit: 5000000,
                    sumSupportMax: 12000000
                },
                {
                    geoId: 225,
                    sumTransit: 2500000,
                    sumSupportMax: 6000000
                }
            ],
            sumMin: 600000,
            sumMax: 20000000,
            costMin: 670000,
            costMax: 50000000,
            costDefault: 10000000,
            periodBaseMin: 3,
            periodBaseMax: 30,
            periodSupportMin: 2,
            periodSupportMax: 20,
            periodDefault: 20,
            downpaymentBaseNewMin: 0.1,
            downpaymentBaseSecondaryMin: 0.2,
            downpaymentBaseMax: 1,
            downpaymentBaseNewTransit: 0.2,
            downpaymentSumTransit: 0.2,
            downpaymentSupportMin: 0.15,
            downpaymentSupportMax: 1,
            downpaymentDefault: 0.3,
            rateBaseNewMin: 0.0859,
            rateBaseNewMax: 0.0929,
            sumMaxReduced: '20000000'
        },
        areParamsLoading: false,
        areParamsFailed: false
    }
};

describe('AlfabankPromoMortgage', () => {
    it('рисует ипотечный калькулятор для строящегося жилья', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <AlfabankPromoMortgage {...props} />
            </AppProvider>,
            { viewport: { width: 940, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ипотечный калькулятор для готового жилья при клике на таб', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <AlfabankPromoMortgage {...props} />
            </AppProvider>,
            { viewport: { width: 940, height: 500 } }
        );

        await page.click('.Radio_type_tag:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
