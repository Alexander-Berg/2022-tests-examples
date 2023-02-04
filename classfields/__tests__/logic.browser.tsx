import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import * as stores from './store';
import { WIDTHS, viewports, selectors } from './constants';
import { Component } from './component';

const specialProjectsWithLogo = [
    ['PIK', 1],
    ['legenda', 10174],
    ['suvarstroit', 11119],
    ['stolicaNizhnij', 11079],
    ['unistroy', 463788],
    ['samolet', 102320],
    ['semya', 241299],
    ['komosstroy', 896162],
    ['yugstroyinvest', 230663],
    ['sadovoeKolco', 75122],
    ['4D', 650428],
    ['kronverk', 524385],
];

advanceTo(new Date('2020-12-31 23:59:59'));

describe('MainMenu', () => {
    describe('Логика отображения', () => {
        const renderOptions = { viewport: viewports[WIDTHS.MEDIUM] };

        it('Нет выпадающего меню у пункта "Яндекс.Аренда"', async () => {
            await render(<Component store={stores.defaultStore} />, renderOptions);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.hover(selectors.tabSelectorFactory(7));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        // https://st.yandex-team.ru/REALTYFRONT-10195
        it.skip('Нет выпадающего меню у пункта "Спец проекты"', async () => {
            await render(<Component store={stores.defaultStore} />, renderOptions);

            await page.hover(selectors.tabSelectorFactory(8));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Всплывающее меню скрывается при уводе курсора с вкладки', async () => {
            await render(<Component store={stores.withoutSpecialProjects} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.hover(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.mouse.move(0, 0);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Всплывающее меню скрывается при уводе курсора с выпадающего меню', async () => {
            await render(<Component store={stores.withoutSpecialProjects} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.hover(selectors.tabSelectorFactory(1));

            await page.hover(selectors.expandedMenuItemFactory(4));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.mouse.move(0, 0);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Переводим фокус таба на всплывающее меню', async () => {
            await render(<Component store={stores.withoutSpecialProjects} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.hover(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.hover(selectors.expandedMenuItemFactory(2));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Яндекс.Аренда недоступна, если текущий регион не Москва и МО', async () => {
            allure.descriptionHtml('state.geo.isInMO -> false');

            await render(<Component store={stores.notMoscow} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Недоступна коммерческая недвижимость', async () => {
            allure.descriptionHtml('state.geo.searchFilters.COMMERCIAL:SELL | COMMERCIAL:RENT  -> undefined');

            await render(<Component store={stores.withoutCommercial} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.hover(selectors.tabSelectorFactory(1));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

            await page.hover(selectors.tabSelectorFactory(2));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Недоступна КП во вкладке "Новостройки"', async () => {
            allure.descriptionHtml('state.geo.hasVillages -> false');

            await render(<Component store={stores.withoutVillages} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            await page.hover(selectors.tabSelectorFactory(3));

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('Без спецпроектов в регионе', async () => {
            allure.descriptionHtml('state.geo.id != state.sitesSpecialProjects.geoId[]');

            await render(<Component store={stores.withoutSpecialProjects} />);

            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it.each(specialProjectsWithLogo)('спецпроект %s с логотипом', async (id, geoId) => {
            await render(<Component store={stores.withLogoSpecialProjects[geoId]} />, {
                viewport: { width: 1000, height: 100 },
            });

            await page.addStyleTag({ content: 'body{padding: 0}' });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
