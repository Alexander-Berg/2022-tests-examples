import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SitePlansSerp, ISitePlansSerpProps } from '..';

const Content = () => <div>Контент выдачи</div>;

describe('SitePlansSerp', () => {
    it('рисует состояние загрузки', async () => {
        const props = { isLoading: true } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует состояние ошибки', async () => {
        const props = { isFailed: true } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует состояние ошибки c доп кнопкой', async () => {
        const props = { isFailed: true, onErrorClick: noop } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу', async () => {
        const props = { count: 0, notFoundTitle: 'Что-то не найдено' } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует пустую выдачу с доп кнопкой', async () => {
        const props = { count: 0, notFoundTitle: 'Что-то не найдено', onNotFoundClick: noop } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу с кнопкой подгрузки', async () => {
        const props = { count: 1, totalCount: 3, loadMoreButtonText: 'Загрузить ещё' } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует выдачу с кнопкой подгрузки (в состоянии загрузки)', async () => {
        const props = { count: 1, isMoreLoading: true } as ISitePlansSerpProps;

        await render(
            <AppProvider>
                <SitePlansSerp {...props}>
                    <Content />
                </SitePlansSerp>
            </AppProvider>,
            { viewport: { width: 320, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
