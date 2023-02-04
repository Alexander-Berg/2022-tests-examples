import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import SerpPageSeparator from '../';

describe('SerpPageSeparator', () => {
    it('Базовая отрисовка', async () => {
        const viewports = [
            { width: 320, height: 150 },
            { width: 700, height: 150 },
        ] as const;

        for (const viewport of viewports) {
            await _render(
                <AppProvider>
                    <SerpPageSeparator page={1} />
                </AppProvider>,
                { viewport }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    });
});
