import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { PriceTrendSubscriptionFormComponent } from '../index';

describe('PriceTrendSubscriptionForm', () => {
    it('default form state and allowPromo checkbox', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('fully filled form', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    email: 'me@hotmail.com',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('with disable button when email is invalid', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    email: 'me@@',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('default form state without allowPromo checkbox', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: false
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('disabled form when checking subscription', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    isChecking: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('disabled form and loading button when form is submitting', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    isSubmitting: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('form in error state', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    hasSubmitError: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('form in success state', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('success state with unconfirmed email', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'me@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('success state with long unconfirmed email', async() => {
        await render(
            <PriceTrendSubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'longemaillongemaillongemaillongemaillongcatlongemail@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 420, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
