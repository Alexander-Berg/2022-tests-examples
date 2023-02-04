import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SeoSiteLinks } from '../index';

import { baseProps, propsWithCoupleSites } from './mocks';

describe('SeoSiteLinks', function () {
    it('Отрисовка с большим количеством предложений', async () => {
        await render(
            <AppProvider>
                <SeoSiteLinks {...baseProps} />
            </AppProvider>,
            { viewport: { width: 480, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка с небольшим количеством предложений', async () => {
        await render(
            <AppProvider>
                <SeoSiteLinks {...propsWithCoupleSites} />
            </AppProvider>,
            { viewport: { width: 480, height: 600 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
