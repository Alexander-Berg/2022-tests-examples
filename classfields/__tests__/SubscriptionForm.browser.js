import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionFormComponent } from '../index';

const SUBSCRIPTION_TYPES = [ [ 'offer' ], [ 'building' ], [ 'search' ] ];

describe('SubscriptionForm', () => {
    it.each(SUBSCRIPTION_TYPES)(
        'should render default form state with allowPromo checkbox (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        shouldShowAllowPromoCheckbox: true,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        }
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it.each(SUBSCRIPTION_TYPES)(
        'should render fully filled form (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        email: 'me@hotmail.com',
                        allowPromo: true,
                        shouldShowAllowPromoCheckbox: true,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        },
                        isEmailValid: true
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it.each(SUBSCRIPTION_TYPES)(
        'should render form with disable button when email is invalid (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        email: 'me@hotmail',
                        allowPromo: true,
                        shouldShowAllowPromoCheckbox: true,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        },
                        isEmailValid: false
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it.each(SUBSCRIPTION_TYPES)(
        'should render default form state without allowPromo checkbox (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        shouldShowAllowPromoCheckbox: false,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        }
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it.each(SUBSCRIPTION_TYPES)(
        'should render disabled form when checking subscription (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        isChecking: true,
                        shouldShowAllowPromoCheckbox: true,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        }
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it.each(SUBSCRIPTION_TYPES)(
        'should render disabled form and loading button when form is submitting (%s subscription type)',
        async subscriptionType => {
            await render(
                <SubscriptionFormComponent
                    formControllerProps={{
                        isSubmitting: true,
                        shouldShowAllowPromoCheckbox: true,
                        subscriptionParams: {
                            title: '1-комнатная квартира',
                            description: 'на окраине мира'
                        }
                    }}
                    subscriptionType={subscriptionType}
                />,
                { viewport: { width: 350, height: 500 } }
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it('should render form in error state', async() => {
        await render(
            <SubscriptionFormComponent
                formControllerProps={{
                    hasSubmitError: true,
                    shouldShowAllowPromoCheckbox: true,
                    subscriptionParams: {
                        title: '1-комнатная квартира',
                        description: 'на окраине мира'
                    }
                }}
                subscriptionType='offer'
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state', async() => {
        await render(
            <SubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
                subscriptionType='offer'
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with unconfirmed email', async() => {
        await render(
            <SubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'me@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
                subscriptionType='offer'
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with long unconfirmed email', async() => {
        await render(
            <SubscriptionFormComponent
                formControllerProps={{
                    subscription: {
                        exists: true,
                        needsConfirmation: true,
                        email: 'longemaillongemaillongemaillongemaillongcatlongemail@hotmail.com'
                    },
                    shouldShowAllowPromoCheckbox: true
                }}
                subscriptionType='offer'
            />,
            { viewport: { width: 350, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
