---
to:  "<%= locals.screenshotTests ? `${cwd}/${name}/__tests__/index.browser.tsx` : null  %>"
---
import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { <%= name %> } from '../';

const mobileViewports = [
    { width: 345, height: 200 },
    { width: 360, height: 200 },
] as const;

const desktopViewports = [
    { width: 1000, height: 200 },
    { width: 1200, height: 200 },
] as const;

const viewports = [...mobileViewports, ...desktopViewports] as const;

const render = async (component: React.ReactElement) => {
    for (const viewport of viewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

describe('<%= name %>', () => {
    it('рендерится корректно', async () => {
        await render(<<%= name %> />);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
