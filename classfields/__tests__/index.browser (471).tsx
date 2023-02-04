import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoOfferLinksBlock } from '../index';

import { baseProps } from './mocks';

describe('SeoOfferLinksBlock', function () {
    it('Отрисовка без изображения', async () => {
        await render(
            <AppProvider>
                <SeoOfferLinksBlock {...baseProps} />
            </AppProvider>,
            { viewport: { width: 480, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с изображением', async () => {
        await render(
            <AppProvider>
                <SeoOfferLinksBlock {...baseProps} withImage />
            </AppProvider>,
            { viewport: { width: 480, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
