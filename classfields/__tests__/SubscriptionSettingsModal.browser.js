import React from 'react';
import { render } from 'jest-puppeteer-react';

// eslint-disable-next-line no-restricted-imports
import { rejectPromise, infinitePromise } from 'realty-www/view/react/libs/test-helpers';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionSettingsScreen } from '../';

const mockSubscription = {
    id: '12345',
    email: 'someone@mail.ru',
    title: 'Подписка на что-то',
    description: 'По такому-то адресу',
    frequency: 60
};

describe('SubscriptionSettingsScreenM', () => {
    it('open modal in default state with 60 min frequency', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 1 min frequency', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={{ ...mockSubscription, frequency: 1 }}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 1440 min frequency', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={{ ...mockSubscription, frequency: 1440 }}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with 10080 min frequency', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={{ ...mockSubscription, frequency: 1 }}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with disabled submit button when email is invalid', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={{ ...mockSubscription, email: 'sdlkfjl' }}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state with disabled submit button when email is empty', async() => {
        await render(
            <SubscriptionSettingsScreen
                subscription={{ ...mockSubscription, email: undefined }}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in loading state after submit button click', async() => {
        await render(
            <SubscriptionSettingsScreen
                onSave={infinitePromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in failed state after rejected submitting', async() => {
        await render(
            <SubscriptionSettingsScreen
                onSave={rejectPromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in default state after rejected submitting and pressing retry afterwards', async() => {
        await render(
            <SubscriptionSettingsScreen
                onSave={rejectPromise()}
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        await page.click('[data-test=subscription-settings-submit-button]');
        await page.waitFor(100);
        await page.click('[data-test=subscription-settings-reset-error]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
