import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import promotionIcon from '@realty-front/icons/colored/promotion-48.svg';
import bindIcon from '@realty-front/icons/colored/bind-48.svg';

import { PaymentHeader } from '../index';

const Component = props => (
    <PaymentHeader icon={promotionIcon} {...props} />
);

describe('PaymentHeader', () => {
    it('title with description, without basePrice', async() => {
        const props = {
            icon: bindIcon,
            title: 'Привязка карты',
            price: 1,
            description: 'Для подтверждения карты мы спишем и вернем 1 рубль'
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('title without description, with basePrice', async() => {
        const props = {
            title: 'Продвижение',
            price: 149,
            basePrice: 666
        };

        const component = <Component {...props} />;

        await render(component, { viewport: { width: 780, height: 150 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
