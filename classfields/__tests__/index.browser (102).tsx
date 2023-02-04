import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { BackCallTermsWithPopup } from '..';

import styles from './styles.module.css';

const views = ['light', 'dark'] as const;

describe('BackCallTermsWithPopup', () => {
    views.forEach((view) => {
        it(`базовая отрисовка ${view}`, async () => {
            await render(<BackCallTermsWithPopup view={view} className={styles[view]} />, {
                viewport: { width: 600, height: 150 },
            });
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`рисует модальное окно с соглашением, после клика на ссылку "согласие" ${view}`, async () => {
            await render(<BackCallTermsWithPopup view={view} className={styles[view]} />, {
                viewport: { width: 600, height: 450 },
            });

            await page.click('.Link');
            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
