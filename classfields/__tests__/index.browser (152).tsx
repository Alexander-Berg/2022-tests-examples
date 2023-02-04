import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageTypes } from 'realty-core/types/mortgage/mortgageProgram';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageBreadcrumbs } from '../';

const initialState = {
    config: {
        origin: 'yandex.ru',
    },
};

const geo = {
    rgid: 741964,
    locative: 'в Москве и МО',
} as never;

describe('MortgageBreadcrumbs', () => {
    it('рисует хлебные крошки для выдачи без фильтра', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <MortgageBreadcrumbs type="mortgage-search" geo={geo} />
            </AppProvider>,
            { viewport: { width: 600, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хлебные крошки для выдачи с фильтром', async () => {
        const searchQuery = {
            mortgageType: [MortgageTypes.MILITARY],
        };

        await render(
            <AppProvider initialState={initialState}>
                <MortgageBreadcrumbs type="mortgage-search" geo={geo} searchQuery={searchQuery} />
            </AppProvider>,
            { viewport: { width: 600, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хлебные крошки для страницы банка', async () => {
        const bank = {
            id: '1',
            name: 'Альфа банк',
            genitiveName: 'Альфа банка',
        };

        await render(
            <AppProvider initialState={initialState}>
                <MortgageBreadcrumbs type="mortgage-bank" geo={geo} bank={bank} />
            </AppProvider>,
            { viewport: { width: 600, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хлебные крошки для страницы программы', async () => {
        const bank = {
            id: '1',
            name: 'Альфа банк',
            genitiveName: 'Альфа банка',
        };

        const program = {
            id: 1,
            programName: 'Ипотека на новостройки',
        };

        await render(
            <AppProvider initialState={initialState}>
                <MortgageBreadcrumbs type="mortgage-program" geo={geo} bank={bank} program={program} />
            </AppProvider>,
            { viewport: { width: 700, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует хлебные крошки для страницы ипотечного калькулятора', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <MortgageBreadcrumbs type="mortgage-calculator" geo={geo} />
            </AppProvider>,
            { viewport: { width: 600, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
