import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageSearchPresets } from '../';
import styles from '../styles.module.css';

import wrapperStyles from './styles.module.css';

const Component = () => (
    <AppProvider>
        <div className={wrapperStyles.container}>
            <MortgageSearchPresets rgid={1} />
        </div>
    </AppProvider>
);

describe('MortgageSearchPresets', () => {
    it('рисует блок с ховером на 1 элементе', async () => {
        await render(<Component />, {
            viewport: { width: 1000, height: 200 },
        });

        await page.hover(`.${styles.item}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует проскроленный блок', async () => {
        await render(<Component />, {
            viewport: { width: 1000, height: 200 },
        });

        await page.click(`.${styles.nextArrow}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует проскроленный до конца блок', async () => {
        await render(<Component />, {
            viewport: { width: 1000, height: 200 },
        });

        await page.click(`.${styles.nextArrow}`);
        await page.click(`.${styles.nextArrow}`);
        await page.click(`.${styles.nextArrow}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
