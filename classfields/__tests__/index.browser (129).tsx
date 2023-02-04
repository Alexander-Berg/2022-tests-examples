import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SiteMdDescription } from '..';
import styles from '../styles.module.css';

import { shortDescCard, longDescCard, mdDescWithHEading, mdDescWithoutHeading } from './mocks';

describe('SiteMdDescription', () => {
    it('Рендерится с коротким описанием без разметки', async () => {
        await render(<SiteMdDescription card={shortDescCard} />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с длинным описанием без разметки', async () => {
        await render(<SiteMdDescription card={longDescCard} />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с длинным описанием без разметки после нажатия на развернуть', async () => {
        await render(<SiteMdDescription card={longDescCard} />, { viewport: { width: 400, height: 300 } });

        await page.click(`.${styles.expandBtn}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Рендерится с разметкой', async () => {
        await render(<SiteMdDescription card={mdDescWithHEading} />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится с разметкой после нажатия на развернуть', async () => {
        await render(<SiteMdDescription card={mdDescWithHEading} />, { viewport: { width: 400, height: 300 } });

        await page.click(`.${styles.expandBtn}`);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Рендерится с разметкой без заголовков', async () => {
        await render(<SiteMdDescription card={mdDescWithoutHeading} />, { viewport: { width: 400, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
