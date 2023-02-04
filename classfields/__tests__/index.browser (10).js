import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PromocodesScreen } from '../index';

const price = {
    base: 1666,
    effective: 345
};
const promocodes = {
    raising: {
        discount: 30,
        count: 1
    },
    promotion: {
        discount: 50,
        count: 1
    },
    premium: {
        discount: 459,
        count: 1
    },
    money: {
        discount: 20,
        count: 1
    }
};

const Component = props => (
    <PromocodesScreen
        promocodes={promocodes}
        price={price}
        onSuccess={() => {}}
        onPaymentClick={() => {}}
        {...props}
    />
);

describe('PromocodesScreen', () => {
    it('Multiple promocodes', async() => {
        const component = <Component />;

        await render(component, { viewport: { width: 780, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Promocodes only payment', async() => {
        const freePrice = {
            base: 0,
            effective: 0
        };
        const component = <Component price={freePrice} />;

        await render(component, { viewport: { width: 780, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
