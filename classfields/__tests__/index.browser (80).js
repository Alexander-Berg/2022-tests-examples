import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import DeveloperCardRewards from '../';

import styles from '../styles.module.css';

import { developerWithRewards, developerWithPartialRewards } from './mocks';

describe('DeveloperCardRewards', () => {
    it('рисует картинки наград', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 320, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует модальное окно с информацией о награде при клике', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 640, height: 300 } }
        );

        await page.click(`.${styles.reward}:nth-child(3)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует модальное окно с информацией о награде и годом при клике', async() => {
        await render(<DeveloperCardRewards developer={developerWithRewards} />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click(`.${styles.reward}:nth-child(1)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('не рисует модальное окно с информацией о награде для награды без наименования и описания', async() => {
        await render(<DeveloperCardRewards developer={developerWithPartialRewards} />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click(`.${styles.reward}:nth-child(1)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует модальное окно с информацией о награде для награды без наименования', async() => {
        await render(<DeveloperCardRewards developer={developerWithPartialRewards} />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click(`.${styles.reward}:nth-child(2)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('рисует модальное окно с информацией о награде для награды без описания', async() => {
        await render(<DeveloperCardRewards developer={developerWithPartialRewards} />,
            { viewport: { width: 400, height: 300 } }
        );

        await page.click(`.${styles.reward}:nth-child(3)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
