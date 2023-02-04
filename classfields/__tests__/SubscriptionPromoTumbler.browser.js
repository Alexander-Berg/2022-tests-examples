import React from 'react';
import { render } from 'jest-puppeteer-react';

// eslint-disable-next-line no-restricted-imports
import { rejectPromise, infinitePromise } from 'realty-www/view/react/libs/test-helpers';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionsPromoTumbler } from '../';

describe('SubscriptionsPromoTumblerMobile', () => {
    it('should render tumbler in enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled />,
            { viewport: { width: 350, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} />,
            { viewport: { width: 350, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in loading-enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled onTogglePromo={infinitePromise()} />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in loading-disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} onTogglePromo={infinitePromise()} />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in error-enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled onTogglePromo={rejectPromise()} />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in error-disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} onTogglePromo={rejectPromise()} />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
