import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PaymentHeaderContainer } from '../container';

const Component = props => (
    <PaymentHeaderContainer {...props} />
);

describe('PaymentHeaderContainer', () => {
    it('combo package for multiple offers', async() => {
        const props = {
            offerIds: new Array(13),
            services: [ 'raising', 'promotion', 'premium' ],
            price: 149,
            basePrice: 255
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('combo package with placement for single offer', async() => {
        const props = {
            offerIds: new Array(1),
            services: [ 'placement', 'raising', 'promotion', 'premium' ],
            price: 149,
            basePrice: 255
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('placement for single offer', async() => {
        const props = {
            offerIds: new Array(1),
            services: [ 'placement' ],
            price: 149,
            basePrice: 255
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('single service for multiple offers', async() => {
        const props = {
            offerIds: new Array(13),
            services: [ 'raising' ],
            price: 149,
            basePrice: 255
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('single service for single offer', async() => {
        const props = {
            offerIds: new Array(1),
            services: [ 'raising' ],
            price: 149,
            basePrice: 255
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
