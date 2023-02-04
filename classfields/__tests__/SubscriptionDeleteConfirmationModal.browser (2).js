import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rejectPromise, infinitePromise } from 'view/react/libs/test-helpers';

import { SubscriptionDeleteConfirmationModal } from '../';

describe('SubscriptionDeleteConfirmationModalD', () => {
    it('open modal in default state', async() => {
        await render(
            <SubscriptionDeleteConfirmationModal
                isOpen
                renderToOverlay
                onClose={() => {}}
            />,
            { viewport: { width: 600, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in loading state after submit button click', async() => {
        await render(
            <SubscriptionDeleteConfirmationModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                onConfirm={infinitePromise()}
            />,
            { viewport: { width: 600, height: 250 } }
        );

        await page.click('[data-test=subscription-delete-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in failed state after rejected submitting', async() => {
        await render(
            <SubscriptionDeleteConfirmationModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                onConfirm={rejectPromise()}
            />,
            { viewport: { width: 600, height: 250 } }
        );

        await page.click('[data-test=subscription-delete-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
