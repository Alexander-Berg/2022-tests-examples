import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import { OfferCardPriceSubscription } from '../index';

import { offer, onClick } from './mocks';

describe('OfferCardPriceSubscription', function () {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <OfferCardPriceSubscription offer={offer} onSubscribeClick={onClick} />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с коротким названием и кастомными стилями кнопки', async () => {
        await render(
            <AppProvider>
                <OfferCardPriceSubscription
                    offer={offer}
                    onSubscribeClick={onClick}
                    buttonView="yellow"
                    buttonSize="xs"
                    useSmallTexts
                />
            </AppProvider>,
            {
                viewport: { width: 400, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('При клике показывается модельное окно', async () => {
        await render(
            <AppProvider>
                <OfferCardPriceSubscription offer={offer} onSubscribeClick={onClick} />
            </AppProvider>,
            {
                viewport: { width: 800, height: 550 },
            }
        );

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
