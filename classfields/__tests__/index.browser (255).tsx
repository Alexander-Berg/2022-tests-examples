import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { IOfferCard } from 'realty-core/types/offerCard';
import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';

import cardFeaturesStyles from 'view/components/CardFeatures/styles.module.css';

import { OfferCardCommercialInfo as OfferCardCommercialInfoBase } from '../';

import { offer1, offer2, offer3 } from './mocks';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const OfferCardCommercialInfo: React.FC<{ offer: any }> = ({ offer }) => (
    <OfferCardCommercialInfoBase offer={offer as IOfferCard} />
);

function showOfferData(offer: AnyObject) {
    return allure.descriptionHtml(`
        <div><strong>Используются следующие данные: </strong></div>
        <pre>
            ${JSON.stringify(offer, undefined, 2)}
        </pre>
    `);
}

describe('OfferCardCommercialInfo', () => {
    it('Рисует описание БЦ (мало данных)', async () => {
        showOfferData(offer1);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCardCommercialInfo offer={offer1} />
            </AppProvider>,
            { viewport: { width: 320, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует описание БЦ (все данные)', async () => {
        showOfferData(offer2);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCardCommercialInfo offer={offer2} />
            </AppProvider>,
            { viewport: { width: 400, height: 1000 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует описание БЦ раскрытая информация', async () => {
        showOfferData(offer3);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCardCommercialInfo offer={offer3} />
            </AppProvider>,
            { viewport: { width: 400, height: 2000 } }
        );

        await page.click(`.${cardFeaturesStyles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рисует описание БЦ раскрытые удобства', async () => {
        showOfferData(offer3);

        await render(
            <AppProvider initialState={{}} context={{}}>
                <OfferCardCommercialInfo offer={offer3} />
            </AppProvider>,
            { viewport: { width: 400, height: 2500 } }
        );

        await page.click(`div:nth-child(3) .${cardFeaturesStyles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
