import React from 'react';
import { Provider } from 'react-redux';
import { render, act } from '@testing-library/react';
import type { RenderResult } from '@testing-library/react';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import Form from 'auto-core/react/components/common/Form/Form';
import type { FormContext, FormValues } from 'auto-core/react/components/common/Form/types';
import AccordionContext from 'auto-core/react/components/common/Accordion/AccordionContext';
import { AccordionContextMock } from 'auto-core/react/components/common/Accordion/AccordionContext.mock';
import type { ActionQueueContext } from 'auto-core/react/components/common/ActionQueue/ActionQueue';
import ActionQueue from 'auto-core/react/components/common/ActionQueue/ActionQueue';
import configMock from 'auto-core/react/dataDomain/config/mock';
import type { AppStateCore } from 'auto-core/react/AppState';
import type { Fields, FieldNames } from 'auto-core/react/components/common/Form/fields/types';

interface Props {
    children: React.ReactElement;
    initialValues?: FormValues<FieldNames, Fields>;
    accordionContext?: typeof AccordionContextMock;
    formApi?: React.RefObject<FormContext<FieldNames, Fields, FieldErrors>>;
    statActionQueueApi?: React.RefObject<ActionQueueContext>;
    storeMock: ReturnType<typeof mockStore>;
    ContextProvider: ReturnType<typeof createContextProvider>;
}

export const TestWrapper = ({
    accordionContext,
    children,
    formApi,
    initialValues = {},
    statActionQueueApi,
    storeMock,
    ContextProvider,
}: Props) => {
    const formApiDefault = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const statActionQueueApiDefault = React.createRef<ActionQueueContext>();

    return (
        <ContextProvider>
            <Provider store={ storeMock }>
                <AccordionContext.Provider value={ accordionContext || AccordionContextMock }>
                    <ActionQueue
                        apiRef={ statActionQueueApi || statActionQueueApiDefault }
                    >
                        <Form<FieldNames, Fields, FieldErrors>
                            apiRef={ formApi || formApiDefault }
                            requiredErrorType={ FieldErrors.REQUIRED }
                            initialValues={ initialValues }
                        >
                            { children }
                        </Form>
                    </ActionQueue>
                </AccordionContext.Provider>
            </Provider>
        </ContextProvider>
    );
};

type RenderComponentProps = Omit<Props, 'children' | 'storeMock' | 'ContextProvider'> &
Partial<Pick<Props, 'storeMock'>> & {
    state?: Partial<AppStateCore>;
    coreContext?: typeof contextMock;
};

export const renderComponent = async(content: JSX.Element, props?: RenderComponentProps) => {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const state = {
        config: configMock.value(),
        ...props?.state,
    };
    const storeMock = props?.storeMock || mockStore(state);

    mockUseSelector(state || {});
    mockUseDispatch(storeMock);

    const ContextProvider = createContextProvider(props?.coreContext || contextMock);

    let result;

    await act(async() => {
        result = render(
            <TestWrapper { ...props } storeMock={ storeMock } ContextProvider={ ContextProvider }>
                { content }
            </TestWrapper>,
        );

        await flushPromises();
    });

    const rerender = (result as unknown as RenderResult)?.rerender;

    return {
        ...(result || {}) as unknown as RenderResult,
        rerender: async(content: JSX.Element, newProps?: RenderComponentProps) => {
            const state = props?.state || {};
            const storeMock = newProps?.storeMock || mockStore(state);

            await act(async() => {
                rerender(
                    <TestWrapper { ...newProps } storeMock={ storeMock } ContextProvider={ ContextProvider }>
                        { content }
                    </TestWrapper>,
                );

                await flushPromises();
            });
        },
    };
};
