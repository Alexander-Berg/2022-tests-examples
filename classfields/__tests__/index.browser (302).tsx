import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoOfferLinks } from '../index';

import { baseProps, commercialBaseProps } from './mocks';

const widths = [[320], [375], [480]];

describe('SeoOfferLinks', function () {
    it('Отрисовка без изображения', async () => {
        await render(
            <AppProvider>
                <SeoOfferLinks {...baseProps} />
            </AppProvider>,
            { viewport: { width: 480, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с изображением', async () => {
        await render(
            <AppProvider>
                <SeoOfferLinks {...baseProps} withImage />
            </AppProvider>,
            { viewport: { width: 480, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.each(widths)('корректно осуществляет перенос счётчика объявлений %d', async (width) => {
        await render(
            <AppProvider>
                <SeoOfferLinks {...commercialBaseProps} withImage />
            </AppProvider>,
            { viewport: { width, height: 600 } }
        );
        await page.addStyleTag({ content: 'body{padding: 16px}' });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
