import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageSearchPageFooterLinks } from '../';
import styles from '../styles.module.css';

const geo = {
    rgid: 1,
};

describe('MortgageSearchPageFooterLinks', () => {
    it('рисует все ссылки', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageSearchPageFooterLinks geo={geo} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рисует ссылку другого цвета при наведении', async () => {
        await render(
            <div style={{ backgroundColor: '#353535' }}>
                <AppProvider initialState={{}}>
                    <MortgageSearchPageFooterLinks geo={geo} />
                </AppProvider>
            </div>,
            { viewport: { width: 1150, height: 200 } }
        );

        await page.hover(`.${styles.column}:nth-child(1) .${styles.link}:nth-child(1)`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
