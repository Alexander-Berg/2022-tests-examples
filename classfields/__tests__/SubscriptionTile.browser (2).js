import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { resolvePromise, rejectPromise, infinitePromise } from 'view/react/libs/test-helpers';

import { SubscriptionTile } from '../';

const mockSubsription = {
    url: 'https://realty.yandex.ru/offer/123',
    id: '12345678',
    title: 'Изменение цены в 2-комнатной квартире',
    description: 'по адресу Москва, Ленина 22',
    email: 'someone@mail.ru',
    isActive: true,
    isConfirmed: true,
    autoconfirmed: false,
    deleted: false
};

describe('SubscriptionTile', () => {
    it('default active subscription', async() => {
        await render(
            <SubscriptionTile subscription={mockSubsription} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default active subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={mockSubsription} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default non-active subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isActive: false }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default non-active subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isActive: false }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default non-confirmed subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isConfirmed: false }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default non-confirmed subscription with long email', async() => {
        await render(
            <SubscriptionTile
                subscription={{
                    ...mockSubsription,
                    isConfirmed: false,
                    email: 'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww@ww.ww'
                }}
            />,
            { viewport: { width: 500, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default non-confirmed subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isConfirmed: false }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default non-confirmed deleted subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isConfirmed: false, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default non-confirmed deleted subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, isConfirmed: false, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default autoconfirmed subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, autoconfirmed: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default autoconfirmed subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, autoconfirmed: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default deleted subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default deleted subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('default autoconfirmed deleted subscription', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, autoconfirmed: true, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default autoconfirmed deleted subscription hovered', async() => {
        await render(
            <SubscriptionTile subscription={{ ...mockSubsription, autoconfirmed: true, deleted: true }} />,
            { viewport: { width: 500, height: 300 } }
        );

        await page.hover('[data-test=subscription-tile-container]');
        await page.waitFor(300);

        expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
    });

    it('non-confirmed subscription in "resend email" loading state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isConfirmed: false }}
                    onResendConfirmation={infinitePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-resend]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('non-confirmed subscription in "resend email" error state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isConfirmed: false }}
                    onResendConfirmation={rejectPromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-resend]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('non-confirmed subscription in "resend email" success state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isConfirmed: false }}
                    onResendConfirmation={resolvePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-resend]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed active subscription in "toggle subscription active" loading state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={mockSubsription}
                    onActiveToggle={infinitePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed active subscription in "toggle subscription active" error state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={mockSubsription}
                    onActiveToggle={rejectPromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed active subscription in "toggle subscription active" success state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={mockSubsription}
                    onActiveToggle={resolvePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed non-active subscription in "toggle subscription active" loading state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isActive: false }}
                    onActiveToggle={infinitePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed non-active subscription in "toggle subscription active" error state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isActive: false }}
                    onActiveToggle={rejectPromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('confirmed non-active subscription in "toggle subscription active" success state', async() => {
        await render(
            (
                <SubscriptionTile
                    subscription={{ ...mockSubsription, isActive: false }}
                    onActiveToggle={resolvePromise()}
                />
            ),
            { viewport: { width: 500, height: 300 } }
        );

        await page.click('[data-test=subscription-tile-active-tumbler]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
