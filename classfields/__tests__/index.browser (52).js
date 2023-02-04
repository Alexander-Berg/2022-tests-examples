import React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import dayjs from '@realty-front/dayjs';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import NaturalVasService from '../index';

const store = createStore(state => state, {
    user: {
        crc: '1',
        isJuridical: false,
        vosUserData: {
            userType: 'OWNER'
        }
    }
});

const defaultServices = {
    raising: { status: 'active', renewal: { status: 'inactive' } },
    promotion: { status: 'active', end: Number(dayjs('1970-01-02')), renewal: { status: 'INACTIVE' } },
    premium: { status: 'active', renewal: { status: 'inactive' } },
    turboSale: { status: 'active', renewal: { status: 'inactive' } }
};

const Service = props => (
    <Provider store={store}>
        <NaturalVasService
            isActive
            hovered={false}
            basePrice={666}
            price={13}
            isRenewalInProcess={false}
            offerId='1'
            isLoading={false}
            services={defaultServices}
            duration={7}
            type={'promotion'}
            isRenewalEnabled={false}
            isTurboRenewal={false}
            isServerSide={false}
            isWideScreen
            now={Number(dayjs('1970-01-01'))}
            onUpdateRenewal={() => {}}
            onRenewalError={() => {}}
            onRenewalStart={() => {}}
            onRenewalEnd={() => {}}
            {...props}
        />
    </Provider>
);

describe('NaturalVasService', () => {
    it('wide screen with base price discount', async() => {
        await render(
            <Service price={13} basePrice={666} />,
            { viewport: { width: 1401, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen without base price discount', async() => {
        await render(
            <Service price={13} basePrice={13} />,
            { viewport: { width: 1401, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen with auto renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={666} isRenewalEnabled services={services} />,
            { viewport: { width: 1401, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen with turbo renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'INACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={666} isRenewalEnabled={false} isTurboRenewal services={services} />,
            { viewport: { width: 1401, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen with base price discount', async() => {
        await render(
            <Service price={13} basePrice={666} isWideScreen={false} />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen without base price discount', async() => {
        await render(
            <Service price={13} basePrice={13} isWideScreen={false} />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen with auto renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={666} isWideScreen={false} isRenewalEnabled services={services} />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen not active', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'UNAVAILABLE' } }
        });

        await render(
            <Service price={13} basePrice={13} isWideScreen={false} isActive={false} services={services} />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen not active hovered', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'UNAVAILABLE' } }
        });

        await render(
            <Service
                services={services}
                price={13}
                basePrice={13}
                isWideScreen={false}
                isActive={false}
                hovered
            />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
