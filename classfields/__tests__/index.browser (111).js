import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import CardPlansOffersSort from '../index';

describe('CardPlansOffersSort', () => {
    it('рисует сортировку по умолчанию (sort = PRICE, direction = DESC)', async() => {
        await render(
            <CardPlansOffersSort
                sort='PRICE'
                direction='ASC'
            />,
            { viewport: { width: 600, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сортировку по умолчанию, hover на первый элемент сортировки', async() => {
        await render(
            <CardPlansOffersSort
                sort='PRICE'
                direction='ASC'
            />,
            { viewport: { width: 600, height: 100 } }
        );

        await page.hover('.CardPlansOffersSort__item');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует сортировку по убыванию цены', async() => {
        await render(
            <CardPlansOffersSort
                sort='PRICE_DESC'
                direction='DESC'
            />,
            { viewport: { width: 600, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сортировку по убыванию этажа', async() => {
        await render(
            <CardPlansOffersSort
                sort='FLOOR_DESC'
                direction='DESC'
            />,
            { viewport: { width: 600, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
