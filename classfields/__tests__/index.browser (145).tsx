import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { YaRentFormControl } from '../';

const geo = {
    hasYandexRent: true,
};

describe('YaRentFormControl', () => {
    it('десктоп рендерится корректно', async () => {
        await render(<YaRentFormControl onChange={noop} geo={geo} />, { viewport: { width: 360, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('тач рендерится корректно', async () => {
        await render(<YaRentFormControl onChange={noop} isMobile geo={geo} />, {
            viewport: { width: 360, height: 200 },
        });

        await page.addStyleTag({ content: 'body{background: #eee; }' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно c выбранным значением', async () => {
        await render(<YaRentFormControl onChange={noop} data="YES" geo={geo} />, {
            viewport: { width: 360, height: 200 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
