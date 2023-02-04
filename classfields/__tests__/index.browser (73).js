import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { CardPlansOffersSort } from '../index';

describe('CardPlansOffersSort', () => {
    it('рисует сортировку по возрастанию цены', async() => {
        await render(
            <CardPlansOffersSort
                sort='PRICE'
                direction='ASC'
            />,
            { viewport: { width: 420, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует сортировку по убыванию этажа', async() => {
        await render(
            <CardPlansOffersSort
                sort='FLOOR'
                direction='DESC'
            />,
            { viewport: { width: 420, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
