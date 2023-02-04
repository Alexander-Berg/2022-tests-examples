import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { DeveloperLandingFooter } from '../';
import styles from '../styles.module.css';

const desktopViewports = [
    { width: 1000, height: 100 },
    { width: 1440, height: 100 },
] as const;

const render = async (component: React.ReactElement) => {
    for (const viewport of desktopViewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const renderOnce = (component: React.ReactElement) => _render(component, { viewport: desktopViewports[0] });

describe('DeveloperLandingFooter', () => {
    it('рендерится корректно', async () => {
        await render(<DeveloperLandingFooter view="samolet" />);
    });

    it('ховер на ссылку', async () => {
        await renderOnce(<DeveloperLandingFooter view="samolet" />);

        await page.hover(`.${styles.link}`);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });
});
