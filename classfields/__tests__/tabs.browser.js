import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Tabs } from '../';

const items = [
    { text: 'tab1', value: 'tab1' },
    { text: 'tab2', value: 'tab2' },
    { text: 'tab3', value: 'tab3' },
    { text: 'tab4', value: 'tab4' },
    { text: 'tab5', value: 'tab5' },
    { text: 'tab6', value: 'tab6' },
    { text: 'tab7', value: 'tab7' },
    { text: 'tab8', value: 'tab8' },
    { text: 'tab9', value: 'tab9' },
    { text: 'tab10', value: 'tab10' }
];

describe('tabs', () => {
    it('should render tabs', async() => {
        await render(
            <Tabs
                items={items}
            />,
            { viewport: { width: 800, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render underlined tabs', async() => {
        await render(
            <Tabs
                items={items}
                underlined
            />,
            { viewport: { width: 800, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tabs with selected tab', async() => {
        await render(
            <Tabs
                items={items}
                activeItemValue='tab6'
                underlined
            />,
            { viewport: { width: 800, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render collapsed tabs', async() => {
        await render(
            <div style={{ maxWidth: 300 }}>
                <Tabs
                    items={items}
                    activeItemValue='tab2'
                    underlined
                />
            </div>,
            { viewport: { width: 400, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render collapsed tabs when selected tab is collapsed', async() => {
        await render(
            <div style={{ maxWidth: 300 }}>
                <Tabs
                    items={items}
                    activeItemValue='tab10'
                    underlined
                />
            </div>,
            { viewport: { width: 400, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render collapsed tabs with popup open', async() => {
        await render(
            <div style={{ maxWidth: 300 }}>
                <Tabs
                    items={items}
                    activeItemValue='tab2'
                    underlined
                />
            </div>,
            { viewport: { width: 400, height: 400 } }
        );

        await page.hover('button');

        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('should render collapsed tabs with popup open when selected tab is collapsed', async() => {
        await render(
            <div style={{ maxWidth: 300 }}>
                <Tabs
                    items={items}
                    activeItemValue='tab10'
                    underlined
                />
            </div>,
            { viewport: { width: 400, height: 400 } }
        );

        await page.hover('button');

        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
