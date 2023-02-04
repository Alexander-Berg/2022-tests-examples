import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionsNonAuthorizedBannerComponent } from '../';

describe('SubscriptionsNonAuthorizedBannerDesktop', () => {
    it('should render non authorized banner', async() => {
        await render(
            <SubscriptionsNonAuthorizedBannerComponent url='https://passport.yandex.ru' />,
            { viewport: { width: 800, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
