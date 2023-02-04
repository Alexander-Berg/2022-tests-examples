import React from 'react';
import { Provider } from 'react-redux';
import type { JestPuppeteerReactRenderOptions } from '@vertis/jest-puppeteer-react';
import { render } from '@vertis/jest-puppeteer-react';
import { Theme, cnTheme } from '@yandex-int/payment-components';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMockBrowser from 'autoru-frontend/mocks/contextMockBrowser';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { AppStateBilling } from 'www-billing/react/AppState';
import { billingContextMockBrowser } from 'www-billing/react/contexts/billing.mock';
import { BillingContextProvider } from 'www-billing/react/contexts/billing';

import '@yandex-int/payment-components/themes/light@desktop.css';

interface Props {
    children: React.ReactElement;
    state?: Partial<AppStateBilling>;
    context?: typeof contextMockBrowser;
}

const themeClassName = cnTheme({
    [Theme.Light]: true,
});

export const TestWrapper = ({
    children,
    context,
    state,
}: Props) => {
    const ContextProvider = createContextProvider(context || contextMockBrowser);
    const store = mockStore(state || {});

    return (
        <ContextProvider>
            <Provider store={ store }>
                <BillingContextProvider value={ billingContextMockBrowser }>
                    <div className={ themeClassName }>
                        { children }
                    </div>
                </BillingContextProvider>
            </Provider>
        </ContextProvider>
    );
};

export const renderComponent = (content: JSX.Element, props?: Omit<Props, 'children'>, options?: JestPuppeteerReactRenderOptions) => {
    return render(
        <TestWrapper { ...props }>
            { content }
        </TestWrapper>,
        options)
    ;
};
