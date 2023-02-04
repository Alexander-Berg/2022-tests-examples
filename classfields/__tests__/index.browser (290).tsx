import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SamoletMenu, SamoletMenuBottom } from '../';
import styles from '../styles.module.css';

import { context, navigate, phone } from './mocks';

const renderComponent = (width = 320) =>
    render(
        <AppProvider context={context}>
            <SamoletMenu navigate={navigate} phone={phone} />
            <SamoletMenuBottom navigate={navigate} />
        </AppProvider>,
        { viewport: { width, height: 650 } }
    );

describe('SamoletMenu', () => {
    it('рендерится (узкий экран)', async () => {
        await renderComponent();
        await page.addStyleTag({ content: 'body{padding: 0}' });
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.header}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится (широкий экран)', async () => {
        await renderComponent(420);
        await page.addStyleTag({ content: 'body{padding: 0}' });
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.header}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
