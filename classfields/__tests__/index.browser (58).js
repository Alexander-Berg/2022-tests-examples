import React from 'react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';

import dayjs from '@realty-front/dayjs';
import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import VasService from '../index';

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
    raising: { status: 'active', renewal: { status: 'not_enabled' } },
    promotion: { status: 'active', end: Number(dayjs('1970-01-02')), renewal: { status: 'INACTIVE' } },
    premium: { status: 'active', renewal: { status: 'not_enabled' } },
    turboSale: { status: 'active', renewal: { status: 'not_enabled' } }
};

const Service = props => (
    <Provider store={store}>
        <VasService
            isActive
            hovered={false}
            basePrice={666}
            price={13}
            isRenewalRequest={false}
            isTurboRenewal={false}
            offerId='1'
            isLoading={false}
            services={defaultServices}
            duration={7}
            type={'promotion'}
            isRenewalEnabled={false}
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

describe('owner-vas-service', () => {
    it('wide screen with base price discount', async() => {
        await render(
            <Service price={13} basePrice={666} />,
            { viewport: { width: 1101, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen with renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={13} isRenewalEnabled services={services} />,
            { viewport: { width: 1101, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen turbo renewal enabled', async() => {
        const services = merge({}, defaultServices, {
            turboSale: { renewal: { status: 'ACTIVE' } },
            promotion: { renewal: { status: 'DISABLED_INACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={13} isRenewalEnabled={false} isTurboRenewal services={services} />,
            { viewport: { width: 1101, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen not active', async() => {
        const services = merge({}, defaultServices, {
            promotion: { status: 'inactive', end: 0, renewal: {
                status: 'UNAVAILABLE'
            } }
        });

        await render(
            <Service price={13} basePrice={13} isActive={false} services={services} />,
            { viewport: { width: 1101, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('wide screen active and unavailable', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'UNAVAILABLE' } }
        });

        await render(
            <Service price={13} basePrice={13} isActive services={services} />,
            { viewport: { width: 1101, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('small screen with renewal and discount', async() => {
        const services = merge({}, defaultServices, {
            promotion: { renewal: { status: 'ACTIVE' } }
        });

        await render(
            <Service price={13} basePrice={666} isRenewalEnabled services={services} discount={50} />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
