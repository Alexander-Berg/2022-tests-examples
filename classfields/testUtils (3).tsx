// todo_tom use test utils for core fields
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

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormPageContextProvider } from 'www-poffer/react/contexts/offerFormPage';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

interface Props {
    children: React.ReactElement;
    initialValues?: FormValues<OfferFormFieldNamesType, OfferFormFields>;
    offerFormContext?: typeof offerFormPageContextMock;
    accordionContext?: typeof AccordionContextMock;
    formApi?: React.RefObject<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;
    statActionQueueApi?: React.RefObject<ActionQueueContext>;
    storeMock: ReturnType<typeof mockStore>;
    ContextProvider: ReturnType<typeof createContextProvider>;
}

export const OfferFormComponentTestWrapper = ({
    accordionContext,
    children,
    formApi,
    initialValues = {},
    offerFormContext,
    statActionQueueApi,
    storeMock,
    ContextProvider,
}: Props) => {
    const formApiDefault = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const statActionQueueApiDefault = React.createRef<ActionQueueContext>();

    return (
        <ContextProvider>
            <Provider store={ storeMock }>
                <OfferFormPageContextProvider value={ offerFormContext || offerFormPageContextMock }>
                    <AccordionContext.Provider value={ accordionContext || AccordionContextMock }>
                        <ActionQueue
                            apiRef={ statActionQueueApi || statActionQueueApiDefault }
                        >
                            <Form<OfferFormFieldNamesType, OfferFormFields, FieldErrors>
                                apiRef={ formApi || formApiDefault }
                                requiredErrorType={ FieldErrors.REQUIRED }
                                initialValues={ initialValues }
                            >
                                { children }
                            </Form>
                        </ActionQueue>
                    </AccordionContext.Provider>
                </OfferFormPageContextProvider>
            </Provider>
        </ContextProvider>
    );
};

type RenderComponentProps = Omit<Props, 'children' | 'storeMock' | 'ContextProvider'> &
Partial<Pick<Props, 'storeMock'>> & {
    state?: Partial<AppState>;
    coreContext?: typeof contextMock;
};

export const renderComponent = async(content: JSX.Element, props?: RenderComponentProps) => {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    const state = {
        offerDraft: offerDraftMock.value(),
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
            <OfferFormComponentTestWrapper { ...props } storeMock={ storeMock } ContextProvider={ ContextProvider }>
                { content }
            </OfferFormComponentTestWrapper>,
        );

        await flushPromises();
    });

    const rerender = (result as unknown as RenderResult)?.rerender;

    return {
        ...(result || {}) as unknown as RenderResult,
        rerender: async(content: JSX.Element, newProps?: RenderComponentProps) => {
            const state = {
                offerDraft: offerDraftMock.value(),
                ...props?.state,
            };
            const storeMock = newProps?.storeMock || mockStore(state);

            await act(async() => {
                rerender(
                    <OfferFormComponentTestWrapper { ...newProps } storeMock={ storeMock } ContextProvider={ ContextProvider }>
                        { content }
                    </OfferFormComponentTestWrapper>,
                );

                await flushPromises();
            });
        },
    };
};
