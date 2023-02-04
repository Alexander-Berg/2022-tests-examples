import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SearchSubscriptionSerpFormComponent } from '../index';

describe('SearchSubscriptionSerpForm', () => {
    it('should render default form state with allowPromo checkbox', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render fully filled form', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    email: 'me@hotmail.com',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form with disable button when email is invalid', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    email: 'me@@',
                    allowPromo: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render default form state without allowPromo checkbox', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    shouldShowAllowPromoCheckbox: false
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form when checking subscription', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    isChecking: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form and loading button when form is submitting', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    isSubmitting: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in error state', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    hasSubmitError: true,
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with unconfirmed email', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'me@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with long unconfirmed email', async() => {
        await render(
            <SearchSubscriptionSerpFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'longemaillongemaillongemaillongemaillongcatlongemail' +
                            'longemaillongemaillongemaillongemaillongcatlongemail' +
                            'longemaillongemaillongemaillongemaillongcatlongemail@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
            />,
            { viewport: { width: 1200, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
