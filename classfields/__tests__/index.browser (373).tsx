import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IOfferCard } from 'realty-core/types/offerCard';
import { AnyObject } from 'realty-core/types/utils';

import { OfferCardCommercialInfo } from '..';

import { offer1, offer2, offer3, offer4 } from './mocks';

const OfferCommercialInfo: React.FC<{ offer: AnyObject }> = ({ offer }) => (
    <OfferCardCommercialInfo offer={offer as IOfferCard} />
);

function showOfferData(offer: AnyObject) {
    return allure.descriptionHtml(`
        <div><strong>Используются следующие данные: </strong></div>
        <pre>
            ${JSON.stringify(offer, undefined, 2)}
        </pre>
    `);
}

describe('OfferCommercialInfo', () => {
    it('Рисует описание БЦ (мало данных)', async () => {
        showOfferData(offer1);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCommercialInfo offer={offer1} />
            </AppProvider>,
            { viewport: { width: 1200, height: 750 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует описание БЦ (все данные)', async () => {
        showOfferData(offer2);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCommercialInfo offer={offer2} />
            </AppProvider>,
            { viewport: { width: 1200, height: 1100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует раскрытое описание БЦ', async () => {
        showOfferData(offer3);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCommercialInfo offer={offer3} />
            </AppProvider>,
            { viewport: { width: 1200, height: 1300 } }
        );

        await page.click('.Link svg');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует описание БЦ без спойлера (<=9 фич)', async () => {
        showOfferData(offer4);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCommercialInfo offer={offer4} />
            </AppProvider>,
            { viewport: { width: 1200, height: 750 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
