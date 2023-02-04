import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperCardAbout from '../';
import styles from '../styles.module.css';

import {
    onlyTitleDeveloper,
    developerWithLogo,
    developerWithAllObjects,
    developerWithSomeObjects,
    developerWithOneItemAtAllObjects,
    developerWithFewRegions,
    developerWithLotRegions,
    developerWithDescription,
    developerWithLongDescription,
    developerWithLongDescriptionAndLotRegions,
    developerWithAllSocialNetworks,
    developerWithSeveralSocialNetworks,
    developerWithSocialNetworksWithoutLogo,
    developerWithDescriptionExtended,
    developerWithLongDescriptionExtended,
    developerWithLongDescriptionAndLotRegionsExtended,
    developerWithContacts
} from './mocks';

const geo = {
    rgid: 741964,
    locative: 'в Москве и МО'
};

describe('DeveloperCardAbout', () => {
    describe('Обычная версия', () => {
        it('рисует заголовок с наименованием застройщика и регионом', async() => {
            await render(<DeveloperCardAbout developer={onlyTitleDeveloper} geo={geo} />,
                { viewport: { width: 800, height: 100 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует логотип застройщика', async() => {
            await render(<DeveloperCardAbout developer={developerWithLogo} geo={geo} />,
                { viewport: { width: 800, height: 120 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика (все)', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllObjects} geo={geo} />,
                { viewport: { width: 1000, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика (для которых есть данные)', async() => {
            await render(<DeveloperCardAbout developer={developerWithSomeObjects} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует бейджики застройщика с единичным домом', async() => {
            await render(<DeveloperCardAbout developer={developerWithOneItemAtAllObjects} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (полное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithDescription} geo={geo} />,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (сокращенное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescription} geo={geo} />,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует полное описание застройщика после раскрытия', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescription} geo={geo} />,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click(`.${styles.readMore}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для регионов под описанием застройщика', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithLongDescriptionAndLotRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click(`.${styles.readMore}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для нескольких регионов', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithFewRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для нескольких регионов и сворачивает лишнее', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithLotRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('отрабатывает клик по кнопке "Ещё"/"Свернуть"', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithLotRegions} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click('span.Link:last-of-type');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click('span.Link:last-of-type');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Расширеная версия', () => {
        it('рисует иконки социальных сетей (все)', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllSocialNetworks} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует иконки социальных сетей (для которых есть данные)', async() => {
            await render(<DeveloperCardAbout developer={developerWithSeveralSocialNetworks} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует иконку социальной сети другого цвета при наведении на нее', async() => {
            await render(<DeveloperCardAbout developer={developerWithAllSocialNetworks} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            await page.hover(`.${styles.socialNetworks} li:nth-child(2)`);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует только иконки социальных сетей если нет логотипа', async() => {
            await render(<DeveloperCardAbout developer={developerWithSocialNetworksWithoutLogo} geo={geo} />,
                { viewport: { width: 800, height: 200 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (полное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithDescriptionExtended} geo={geo} />,
                { viewport: { width: 800, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует описание застройщика (сокращенное)', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescriptionExtended} geo={geo} />,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует полное описание застройщика после раскрытия', async() => {
            await render(<DeveloperCardAbout developer={developerWithLongDescriptionExtended} geo={geo} />,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click(`.${styles.readMore}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопки для регионов под описанием застройщика', async() => {
            await render(
                <AppProvider>
                    <DeveloperCardAbout developer={developerWithLongDescriptionAndLotRegionsExtended} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click(`.${styles.readMore}`);

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        });

        it('рисует кнопки с контактами застройщика', async() => {
            const initialState = {
                backCall: {
                    status: '',
                    phone: '',
                    savedPhone: ''
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <DeveloperCardAbout developer={developerWithContacts} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует кнопку с контактами застройщика другого цвета при наведении на нее', async() => {
            const initialState = {
                backCall: {
                    status: '',
                    phone: '',
                    savedPhone: ''
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <DeveloperCardAbout developer={developerWithContacts} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            await page.hover(`.${styles.phoneButton}`);

            expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
        });

        it('рисует кнопку с номером застройщика после клика на ней', async() => {
            const initialState = {
                backCall: {
                    status: '',
                    value: '',
                    savedPhone: ''
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <DeveloperCardAbout developer={developerWithContacts} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            await page.click(`.${styles.phoneButton}`);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует форму обратного звонка с предзаполненным номером телефона', async() => {
            const initialState = {
                user: {
                    defaultPhone: '+79992345643'
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <DeveloperCardAbout developer={developerWithContacts} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('рисует форму обратного звонка с оформленной заявкой', async() => {
            const initialState = {
                backCallApi: {
                    DEVELOPER: {
                        1: { sourceNumber: '+79998877639' }
                    }
                }
            };

            await render(
                <AppProvider initialState={initialState}>
                    <DeveloperCardAbout developer={developerWithContacts} geo={geo} />
                </AppProvider>,
                { viewport: { width: 900, height: 400 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
