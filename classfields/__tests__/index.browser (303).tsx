import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoSiteLinks } from '../index';

import { baseProps } from './mocks';

describe('SeoSiteLinks', function () {
    it('Отрисовка без изображения', async () => {
        await render(
            <AppProvider>
                <SeoSiteLinks {...baseProps} />
            </AppProvider>,
            { viewport: { width: 480, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
