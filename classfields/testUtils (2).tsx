import React from 'react';
import { Provider } from 'react-redux';
import { render, act } from '@testing-library/react';
import type { RenderResult } from '@testing-library/react';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { AppStateBilling } from 'www-billing/react/AppState';
import { billingContextMock } from 'www-billing/react/contexts/billing.mock';
import { BillingContextProvider } from 'www-billing/react/contexts/billing';

interface WrapperProps {
    children: React.ReactElement;
    store: ReturnType<typeof mockStore>;
    ContextProvider: ReturnType<typeof createContextProvider>;
}

export const TestWrapper = ({
    children,
    ContextProvider,
    store,
}: WrapperProps) => {

    return (
        <ContextProvider>
            <Provider store={ store }>
                <BillingContextProvider value={ billingContextMock }>
                    { children }
                </BillingContextProvider>
            </Provider>
        </ContextProvider>
    );
};

type ComponentProps = {
    state?: Partial<AppStateBilling>;
    context?: typeof contextMock;
}

export const renderComponent = async(content: JSX.Element, props?: ComponentProps) => {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const state = props?.state || {};
    const storeMock = mockStore(state);

    mockUseSelector(state || {});
    mockUseDispatch(storeMock);

    const ContextProvider = createContextProvider(props?.context || contextMock);

    let result = {} as RenderResult;

    await act(async() => {
        result = render(
            <TestWrapper { ...props } store={ storeMock } ContextProvider={ ContextProvider }>
                { content }
            </TestWrapper>,
        );

        await flushPromises();
    });

    return {
        ...result,
        rerender: async(content: JSX.Element, newProps?: ComponentProps) => {
            const state = props?.state || {};
            const storeMock = mockStore(state);

            await act(async() => {
                result.rerender(
                    <TestWrapper { ...newProps } store={ storeMock } ContextProvider={ ContextProvider }>
                        { content }
                    </TestWrapper>,
                );

                await flushPromises();
            });
        },
    };
};
