import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardPlansOffers } from '../index';

import { getInitialState } from './mocks';

describe('CardPlansOffers', () => {
    it('рисует таблицу офферов с фильтром и сортировкой', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <CardPlansOffers />
            </AppProvider>,
            { viewport: { width: 360, height: 700 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу офферов с фильтром без сортировки', async() => {
        await render(
            <AppProvider
                initialState={
                    getInitialState({
                        selectedHouses: [ '1990069', '1990070' ],
                        totalItems: 3
                    })
                }
            >
                <CardPlansOffers />
            </AppProvider>,
            { viewport: { width: 360, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует таблицу офферов без сортировки и фильтра', async() => {
        await render(
            <AppProvider
                initialState={
                    getInitialState({
                        totalItems: 3
                    })
                }
            >
                <CardPlansOffers />
            </AppProvider>,
            { viewport: { width: 360, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует состояние загрузки', async() => {
        await render(
            <AppProvider initialState={getInitialState({ isLoading: true, pager: null })}>
                <CardPlansOffers />
            </AppProvider>,
            { viewport: { width: 360, height: 120 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ошибку', async() => {
        await render(
            <AppProvider initialState={getInitialState({ error: true })}>
                <CardPlansOffers />
            </AppProvider>,
            { viewport: { width: 360, height: 280 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
