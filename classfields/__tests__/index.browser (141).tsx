import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SearchSubscriptionFormControlForm } from '../';

const options = {
    viewport: { width: 500, height: 350 },
};

const commonProps = {
    onCloseModal: () => undefined,

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

describe('SearchSubscriptionFormControlForm', () => {
    it('Успех', async () => {
        await render(
            <AppProvider>
                <SearchSubscriptionFormControlForm
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
            options
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Нужно подтверждение', async () => {
        await render(
            <AppProvider>
                <SearchSubscriptionFormControlForm
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
            options
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
