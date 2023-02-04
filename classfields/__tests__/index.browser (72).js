import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardPlansOffersSerp } from '../index';

import { getOffers, getOffersWithVirtualTour, getInitialState, getPager } from './mocks';

describe('CardPlansOffersSerp', () => {
    it('рисует таблицу с офферами', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(0, 3)}
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами в состоянии загрузки', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(0, 3)}
                    isLoading
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами с пагинатором (1 страница)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(0, 5)}
                    pager={getPager()}
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами с пагинатором (последняя страница)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(5, 6)}
                    pager={getPager({ page: 1 })}
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует зафильтрованную таблицу с офферами', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(5, 6)}
                    pager={getPager({ page: 1 })}
                    totalOffers={10}
                    withShowAll
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 320 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу с офферами с изображениями планировок и 3д туром', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffersWithVirtualTour().slice(0, 2)}
                    withPlanImages
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует зафильтрованную таблицу с офферами (апартаменты)', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffersSerp
                    offers={getOffers().slice(5, 6)}
                    pager={getPager({ page: 1 })}
                    totalOffers={10}
                    isApartment
                    withShowAll
                />
            </AppProvider>
            ,
            { viewport: { width: 360, height: 320 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
