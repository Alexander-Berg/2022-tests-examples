import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AlfabankPromoTerms } from '../';

describe('AlfabankPromoTerms', () => {
    it('рисует общие условия в расширенном блоке с условиями ипотеки', async() => {
        await render(
            <AlfabankPromoTerms />,
            { viewport: { width: 940, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует базовые ставки в расширенном блоке с условиями ипотеки при клике на таб', async() => {
        await render(
            <AlfabankPromoTerms />,
            { viewport: { width: 940, height: 900 } }
        );

        await page.click('.Radio_type_tag:nth-child(2)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует основые требования в расширенном блоке с условиями ипотеки при клике на таб', async() => {
        await render(
            <AlfabankPromoTerms />,
            { viewport: { width: 940, height: 900 } }
        );

        await page.click('.Radio_type_tag:nth-child(3)');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
