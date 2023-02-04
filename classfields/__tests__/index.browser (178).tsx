import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { PageName } from 'realty-core/types/router';

import { SplashBanner, BANNER_VARIANTS } from '../index';

describe('SplashBanner', () => {
    const configFieldsMock = {
        rootUrl: '',
        retpath: '',
    };
    const page: PageName = 'index';
    const bannersNames = Object.keys(BANNER_VARIANTS);

    bannersNames.forEach((banner) => {
        const pageParams: Record<string, string> = {
            appBannerType: banner,
        };
        it(`Рендерит баннер ${banner}`, async function () {
            await render(
                <AppProvider>
                    <SplashBanner
                        config={{
                            serverTimeStamp: 1,
                            ...configFieldsMock,
                        }}
                        pageName={page}
                        pageParams={pageParams}
                    />
                </AppProvider>,
                { viewport: { width: 320, height: 570 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
