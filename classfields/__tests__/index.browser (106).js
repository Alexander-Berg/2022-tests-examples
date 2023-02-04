import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/react/libs/test-helpers';

import DeveloperBreadcrumbs from '../';

const initialState = {
    config: {
        origin: 'yandex.ru'
    }
};

const developer = {
    id: '1',
    name: 'Самолет'
};

const geo = {
    rgid: 741964,
    locative: 'в Москве и МО'
};

describe('DeveloperBreadcrumbs', () => {
    it('рисует хлебные крошки с текущим застройщиком и регионом + списком застройщиков в регионе', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperBreadcrumbs developer={developer} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 50 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ссылки другого цвета при наведении', async() => {
        await render(
            <AppProvider initialState={initialState}>
                <DeveloperBreadcrumbs developer={developer} geo={geo} />
            </AppProvider>,
            { viewport: { width: 1000, height: 50 } }
        );

        await page.hover('.Breadcrumbs__item:nth-child(1)');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();

        await page.hover('.Breadcrumbs__item:nth-child(2)');

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
