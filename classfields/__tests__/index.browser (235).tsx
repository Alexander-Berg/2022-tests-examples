import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramCardFeatures } from '../';
import styles from '../styles.module.css';

import { firstProgram, secondProgram } from './mocks';

describe('MortgageProgramCardFeatures', () => {
    it('рисует блок (минимальный набор данных)', async () => {
        await render(<MortgageProgramCardFeatures card={firstProgram} />, {
            viewport: { width: 320, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (максимальный набор данных)', async () => {
        await render(<MortgageProgramCardFeatures card={secondProgram} />, {
            viewport: { width: 400, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует информацию о ставке', async () => {
        await render(<MortgageProgramCardFeatures card={firstProgram} />, {
            viewport: { width: 400, height: 400 },
        });

        await page.click(`.${styles.rateInfoIcon}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
