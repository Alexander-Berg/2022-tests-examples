import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageSearchSeoText } from '../';
import styles from '../styles.module.css';

describe('MortgageSearchSeoText', () => {
    it('рисует блок', async () => {
        await render(<MortgageSearchSeoText />, {
            viewport: { width: 1100, height: 1600 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(1) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(2) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(3) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(4) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(5) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок для семейной ипотеки', async () => {
        await render(<MortgageSearchSeoText textType="young-family" />, {
            viewport: { width: 1100, height: 1600 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 15; i++) {
            await page.click(`.${styles.accordionEntry}:nth-child(${i}) .${styles.accordionHeader}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('рисует блок для военной ипотеки', async () => {
        await render(<MortgageSearchSeoText textType="military" />, {
            viewport: { width: 1100, height: 1400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 11; i++) {
            await page.click(`.${styles.accordionEntry}:nth-child(${i}) .${styles.accordionHeader}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('рисует блок для ипотеки с гос. поддержкой', async () => {
        await render(<MortgageSearchSeoText textType="state-support" />, {
            viewport: { width: 1100, height: 2000 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 18; i++) {
            await page.click(`.${styles.accordionEntry}:nth-child(${i}) .${styles.accordionHeader}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('рисует блок для сельской ипотеки', async () => {
        await render(<MortgageSearchSeoText textType="village" />, {
            viewport: { width: 1100, height: 1300 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 9; i++) {
            await page.click(`.${styles.accordionEntry}:nth-child(${i}) .${styles.accordionHeader}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });

    it('рисует блок для ипотеки с материнским капиталом', async () => {
        await render(<MortgageSearchSeoText textType="mothers-capital" />, {
            viewport: { width: 1100, height: 1500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        for (let i = 1; i <= 11; i++) {
            await page.click(`.${styles.accordionEntry}:nth-child(${i}) .${styles.accordionHeader}`);
            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });
});
