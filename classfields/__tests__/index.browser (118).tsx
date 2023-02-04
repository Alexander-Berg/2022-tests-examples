import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferEGRNPaidPromotionSmall } from '../index';

const WIDTH_SMALL = 320;
const WIDTH_LANDSCAPE = 670;

const store = {
    config: { origin: 'yandex.ru' },
    routing: { locationBeforeTransitions: { search: '', pathname: '' } },
    user: {},
};

const TEST_CASES = [
    { price: 200, description: 'should render promotion without price modifiers' },
    { price: 200, basePrice: 250, description: 'should render promotion with base price' },
    { price: 100, basePrice: 50, description: 'should render negative discount' },
    {
        description: 'отрисовка в большом размере с базовой ценой',
        price: 100,
        basePrice: 150,
        purchaseButtonProps: { size: 'xl', type: 'button' },
    },
] as const;

describe('OfferEGRNPaidPromotionSmall', () => {
    TEST_CASES.forEach(({ description, ...props }) => {
        it(`${description}, small width`, async () => {
            await render(
                <AppProvider initialState={store}>
                    <OfferEGRNPaidPromotionSmall {...props} />
                </AppProvider>,
                { viewport: { width: WIDTH_SMALL, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${description}, landscape`, async () => {
            await render(
                <AppProvider initialState={store}>
                    <OfferEGRNPaidPromotionSmall {...props} />
                </AppProvider>,
                { viewport: { width: WIDTH_LANDSCAPE, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
