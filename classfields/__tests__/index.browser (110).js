import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import CardPlansOffersSerp from '../index';

import { offers, initialState } from './mocks';

describe('CardPlansOffersSerp', () => {
    it('рисует таблицу с офферами', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffersSerp
                    price={4730726}
                    offers={offers}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами без колонки с разницей цен', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffersSerp
                    offers={offers}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами, c hover на первой строке', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffersSerp
                    price={4730726}
                    offers={offers}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        await page.hover('.CardPlansOffersSerp__link');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами, c hover на иконке "добавить в избранное"', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <CardPlansOffersSerp
                    price={4730726}
                    offers={offers}
                />
            </AppProvider>
            ,
            { viewport: { width: 800, height: 600 } }
        );

        await page.hover('.CardPlansOffersSerp__favorites');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
