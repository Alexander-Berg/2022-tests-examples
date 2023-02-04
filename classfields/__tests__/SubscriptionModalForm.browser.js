import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionModalFormComponent } from '../index';

describe('SubscriptionModalForm', () => {
    it('should render default form state with allowPromo checkbox', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                shouldShowAllowPromoCheckbox
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render fully filled form', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                email='me@hotmail.com'
                allowPromo
                shouldShowAllowPromoCheckbox
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form with disable button when email is invalid', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                email='me@@'
                allowPromo
                shouldShowAllowPromoCheckbox
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render default form state without allowPromo checkbox', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                shouldShowAllowPromoCheckbox={false}
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form when checking subscription', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                shouldShowAllowPromoCheckbox
                isChecking
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render disabled form and loading button when form is submitting', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                shouldShowAllowPromoCheckbox
                isSubmitting
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in error state', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                hasSubmitError
                shouldShowAllowPromoCheckbox
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                subscription={{
                    exists: true
                }}
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
                shouldShowAllowPromoCheckbox
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with unconfirmed email', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                subscription={{
                    exists: true,
                    needsConfirmation: true,
                    email: 'me@hotmail.com'
                }}
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
                shouldShowAllowPromoCheckbox
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render form in success state with long unconfirmed email', async() => {
        await render(
            <SubscriptionModalFormComponent
                subscriptionType='offer'
                subscription={{
                    exists: true,
                    needsConfirmation: true,
                    email: 'longemaillongemaillongemaillongemaillongcatlongemail@hotmail.com'
                }}
                subscriptionParams={{
                    title: '1-комнатная квартира',
                    description: 'на окраине мира'
                }}
                shouldShowAllowPromoCheckbox
            />,
            { viewport: { width: 520, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
