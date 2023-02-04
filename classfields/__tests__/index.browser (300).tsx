import React from 'react';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SearchSubscriptionForm } from '../';

const commonProps = {
    onSuccessButtonClick: () => undefined,

    email: '',
    onEmailChange: () => undefined,
    isEmailValid: false,

    title: '',
    onTitleChange: () => undefined,

    isChecking: false,
    isSubmitting: false,
    hasSubmitError: false,

    subscriptionParams: {
        title: '',
        description: '',
    },

    onSubmit: () => undefined,
    onResetFormError: () => undefined,

    allowPromo: true,
    onAllowPromoChange: () => undefined,
};

describe('SearchSubscriptionForm', () => {
    it(`Успех`, async () => {
        await render(
            <AppProvider>
                <SearchSubscriptionForm
                    {...commonProps}
                    subscription={{
                        id: '',
                        userRef: '',
                        email: '',
                        frequency: 1,
                        needsConfirmation: false,
                        exists: true,
                    }}
                />
            </AppProvider>,
            {
                viewport: { width: 420, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Нужно подтверждение`, async () => {
        await render(
            <AppProvider>
                <SearchSubscriptionForm
                    {...commonProps}
                    subscription={{
                        id: '',
                        userRef: '',
                        email: 'me@hotmail.com',
                        frequency: 1,
                        needsConfirmation: true,
                        exists: true,
                    }}
                />
            </AppProvider>,
            {
                viewport: { width: 340, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
