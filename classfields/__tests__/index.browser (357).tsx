import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { MortgageProgramCardFeatures } from '../';
import styles from '../styles.module.css';

import { firstProgram, secondProgram } from './mocks';

describe('MortgageProgramCardFeatures', () => {
    it('рисует блок (минимальный набор данных)', async () => {
        await render(<MortgageProgramCardFeatures card={firstProgram} />, {
            viewport: { width: 1100, height: 250 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует блок (максимальный набор данных)', async () => {
        await render(<MortgageProgramCardFeatures card={secondProgram} />, {
            viewport: { width: 960, height: 250 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ховер на информации о ставке', async () => {
        await render(<MortgageProgramCardFeatures card={firstProgram} />, {
            viewport: { width: 1100, height: 250 },
        });

        await page.hover(`.${styles.rateInfoIcon}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
