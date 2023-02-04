import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rejectPromise, infinitePromise } from 'view/react/libs/test-helpers';

import { SubscriptionsPromoTumbler } from '../';

describe('SubscriptionsPromoTumbler', () => {
    it('should render tumbler in enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled />,
            { viewport: { width: 450, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} />,
            { viewport: { width: 450, height: 200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in loading-enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled onTogglePromo={infinitePromise()} />,
            { viewport: { width: 450, height: 200 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in loading-disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} onTogglePromo={infinitePromise()} />,
            { viewport: { width: 450, height: 200 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in error-enabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled onTogglePromo={rejectPromise()} />,
            { viewport: { width: 450, height: 200 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tumbler in error-disabled state', async() => {
        await render(
            <SubscriptionsPromoTumbler promoEnabled={false} onTogglePromo={rejectPromise()} />,
            { viewport: { width: 450, height: 200 } }
        );

        await page.click('[data-test=subscriptions-promo-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
