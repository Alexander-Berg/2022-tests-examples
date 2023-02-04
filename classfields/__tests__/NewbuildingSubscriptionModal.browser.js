import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { NewbuildingSubscriptionModalComponent } from '../index';

describe('Modal', () => {
    it('should render default form state with allowPromo checkbox', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render fully filled form', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    email: 'me@hotmail.com',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form with disable button when email is invalid', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    email: 'me@@',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render default form state without allowPromo checkbox', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: false,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form when checking subscription', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    isChecking: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form and loading button when form is submitting', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    isSubmitting: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in error state', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    hasSubmitError: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: { title: 'ЖК "Мой"' }
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    subscription: {
                        exists: true
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with unconfirmed email', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'me@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with long unconfirmed email', async() => {
        await render(
            <NewbuildingSubscriptionModalComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'longemaillongemaillongemaillongemaillongcatlongemail@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 500, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
