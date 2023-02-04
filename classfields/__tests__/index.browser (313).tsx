import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansV2 } from '..';

import { getProps } from './mocks';

describe('SitePlansV2', () => {
    it('основной блок', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 240 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка с выдачей', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('фильтр в модалке', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');
        await page.click('[class*=IconSvg_filters]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка с пустой выдачей', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} plans={[]} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка c кнопкой подгрузки выдачи', async () => {
        const props = getProps();

        await render(
            <AppProvider>
                <SitePlansV2 {...props} plansCount={props.plans.length + 1} />
            </AppProvider>,
            { viewport: { width: 320, height: 1100 } }
        );

        await page.click('[class*=chooseButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка в состоянии загрузки', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} arePlansLoading />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка c ошибкой', async () => {
        await render(
            <AppProvider>
                <SitePlansV2 {...getProps()} arePlansFailed />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('модалка c офферами', async () => {
        await render(
            <AppProvider initialState={{ user: { favorites: [], favoritesMap: {} } }}>
                <SitePlansV2 {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 1000 } }
        );

        await page.click('[class*=chooseButton]');
        await page.click('[class*=offersShowButton]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
