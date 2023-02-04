import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramCardDescription } from '../';
import styles from '../styles.module.css';

import { firstProgram, secondProgram } from './mocks';

describe('MortgageProgramCardDescription', () => {
    it('рисует блок (минимальный набор данных)', async () => {
        await render(<MortgageProgramCardDescription card={firstProgram} />, {
            viewport: { width: 320, height: 500 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(1) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(2) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(3) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (максимальный набор данных)', async () => {
        await render(<MortgageProgramCardDescription card={secondProgram} />, {
            viewport: { width: 400, height: 2200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(1) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(2) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.accordionEntry}:nth-child(3) .${styles.accordionHeader}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
