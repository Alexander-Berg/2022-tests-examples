import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SitePlansSort } from '..';

describe('SitePlansSort', () => {
    it('рисует выбранную сортировку по цене', async() => {
        await render(
            <SitePlansSort sort='PRICE' />,
            { viewport: { width: 400, height: 80 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выбранную сортировку по площади', async() => {
        await render(
            <SitePlansSort sort='AREA_DESC' />,
            { viewport: { width: 400, height: 80 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выбранную сортировку по цене за м²', async() => {
        await render(
            <SitePlansSort sort='PRICE_PER_SQUARE' />,
            { viewport: { width: 400, height: 80 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
