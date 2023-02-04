import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardAbout from '../';

import {
    onlyTitleDeveloper,
    developerWithLogo,
    developerWithAllObjects,
    developerWithSomeObjects,
    developerWithOneItemAtAllObjects,
    developerWithDescription,
    developerWithLongDescription,
    developerWithFewRegions,
    developerWithAllSocialNetworks,
    developerWithSeveralSocialNetworks,
    developerWithAllData,
    developerWithLongDescriptionAndRegions
} from './mocks';

const geo = {
    rgid: 741964,
    locative: 'в Москве и МО'
};

describe('DeveloperCardAbout', () => {
    describe('Обычная версия', () => {
        it('рисует заголовок с наименованием застройщика и регионом', async() => {
            await render(<DeveloperCardAbout developer={onlyTitleDeveloper} geo={geo} />,
                { viewport: { width: 320, height: 100 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует логотип застройщика', async() => {
            await render(<DeveloperCardAbout developer={developerWithLogo} geo={geo} />,
                { viewport: { width: 400, height: 100 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика (все)', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllObjects} geo={geo} />,
                { viewport: { width: 400, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика (для которых есть данные)', async() => {
            await render(<DeveloperCardAbout developer={developerWithSomeObjects} geo={geo} />,
                { viewport: { width: 400, height: 300 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика с единичным домом', async() => {
            await render(<DeveloperCardAbout developer={developerWithOneItemAtAllObjects} geo={geo} />,
                { viewport: { width: 400, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (полное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithDescription} geo={geo} />,
                { viewport: { width: 640, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (сокращенное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescription} geo={geo} />,
                { viewport: { width: 400, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует полное описание застройщика после раскрытия', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescription} geo={geo} />,
                { viewport: { width: 400, height: 700 } }
            );

            await page.click('.Shorter__expander');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для застройщика с несколькими регионами', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithFewRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 400, height: 700 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для застройщика с несколькими регионами под описанием', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithLongDescriptionAndRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 400, height: 700 } }
            );

            await page.click('.Shorter__expander');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Расширеная версия', () => {
        it('рисует всю информацию о застройщике', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllData} geo={geo} />,
                { viewport: { width: 450, height: 600 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует иконки социальных сетей (все)', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllSocialNetworks} geo={geo} />,
                { viewport: { width: 450, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует иконки социальных сетей (для которых есть данные)', async() => {
            await render(<DeveloperCardAbout developer={developerWithSeveralSocialNetworks} geo={geo} />,
                { viewport: { width: 400, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
