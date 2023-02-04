import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import dayjs from '@realty-front/dayjs';

import RectTurboService from '../RectTurboService';

const defaultServices = {
    raising: { status: 'active', renewal: { status: 'INACTIVE' } },
    promotion: { status: 'active', end: Number(dayjs('1970-01-02')), renewal: { status: 'INACTIVE' } },
    premium: { status: 'active', renewal: { status: 'INACTIVE' } },
    turboSale: { status: 'active', end: Number(dayjs('1970-01-02')), renewal: { status: 'INACTIVE' } }
};

const discount = 50;

const defaultProps = {
    services: defaultServices,
    duration: 7,
    discount: null,
    price: 13,
    basePrice: 666,
    priceBeforeDiscount: 666,
    isActive: true,
    isLoading: false,
    isRenewalEnabled: false,
    isAnyRenewalInProcess: false,
    timestamp: 0,
    onButtonClick: () => {},
    onUpdateRenewal: () => {},
    onRenewalError: () => {}
};

const RectService = props => (
    <RectTurboService
        {...defaultProps}
        {...props}
    />
);

describe('RectTurboService', () => {
    it('with base price discount', async() => {
        await render(
            <RectService price={13} basePrice={666} priceBeforeDiscount={666} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            turboSale: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <RectService price={13} basePrice={666} priceBeforeDiscount={666} isRenewalEnabled services={services} />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('not active', async() => {
        const services = merge({}, defaultServices, {
            turboSale: {
                status: 'inactive', end: 0,
                renewal: {
                    status: 'UNAVAILABLE'
                }
            }
        });

        await render(
            <RectService
                price={13}
                basePrice={13}
                priceBeforeDiscount={13}
                isActive={false}
                services={services}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with renewal and discount', async() => {
        const services = merge({}, defaultServices, {
            turboSale: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <RectService
                price={13}
                basePrice={666}
                priceBeforeDiscount={666}
                isRenewalEnabled
                services={services}
                discount={discount}
            />,
            { viewport: { width: 1000, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('not active, wide screen', async() => {
        const services = merge({}, defaultServices, {
            turboSale: {
                status: 'inactive', end: 0,
                renewal: {
                    status: 'UNAVAILABLE'
                }
            }
        });

        await render(
            <RectService
                price={13}
                basePrice={13}
                priceBeforeDiscount={13}
                isActive={false}
                services={services}
            />,
            { viewport: { width: 1400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with renewal and discount, wide screen', async() => {
        const services = merge({}, defaultServices, {
            turboSale: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <RectService
                price={13}
                basePrice={666}
                priceBeforeDiscount={666}
                isRenewalEnabled
                services={services}
                discount={discount}
            />,
            { viewport: { width: 1400, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
