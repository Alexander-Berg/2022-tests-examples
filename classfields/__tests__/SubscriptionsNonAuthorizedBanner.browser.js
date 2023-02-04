import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionsNonAuthorizedBannerComponent } from '../';

describe('SubscriptionsNonAuthorizedBannerMobile', () => {
    it('should render non authorized banner', async() => {
        await render(
            <SubscriptionsNonAuthorizedBannerComponent url='https://passport.yandex.ru' />,
            { viewport: { width: 350, height: 150 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
