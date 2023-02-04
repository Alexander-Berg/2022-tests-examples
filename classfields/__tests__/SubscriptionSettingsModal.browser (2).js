import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { rejectPromise, infinitePromise } from 'view/react/libs/test-helpers';

import { SubscriptionSettingsModal } from '../';

const mockSubscription = {
    id: '12345',
    email: 'someone@mail.ru',
    title: 'Подписка на что-то',
    description: 'По такому-то адресу',
    frequency: 60
};

describe('SubscriptionSettingsModalDesktop', () => {
    it('open modal in default state with 60 min frequency', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={mockSubscription}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 1 min frequency', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={{ ...mockSubscription, frequency: 1 }}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 1440 min frequency', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={{ ...mockSubscription, frequency: 1440 }}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 10080 min frequency', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={{ ...mockSubscription, frequency: 1 }}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with disabled submit button when email is invalid', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={{ ...mockSubscription, email: 'sdlkfjl' }}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with disabled submit button when email is empty', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                subscription={{ ...mockSubscription, email: undefined }}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in loading state after submit button click', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                onSave={infinitePromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in failed state after rejected submitting', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                onSave={rejectPromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state after rejected submitting and pressing retry afterwards', async() => {
        await render(
            <SubscriptionSettingsModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                onSave={rejectPromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 600, height: 500 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');
        await page.waitFor(100);
        await page.click('[data-test=subscription-settings-reset-error]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
