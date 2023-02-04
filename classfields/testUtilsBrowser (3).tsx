// todo_tom use test utils for core fields
import React from 'react';
import { Provider } from 'react-redux';
import type { JestPuppeteerReactRenderOptions } from '@vertis/jest-puppeteer-react';
import { render } from '@vertis/jest-puppeteer-react';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMockBrowser from 'autoru-frontend/mocks/contextMockBrowser';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import Form from 'auto-core/react/components/common/Form/Form';
import type { FormContext, FormValues } from 'auto-core/react/components/common/Form/types';
import type { ActionQueueContext } from 'auto-core/react/components/common/ActionQueue/ActionQueue';
import AccordionContext from 'auto-core/react/components/common/Accordion/AccordionContext';
import { AccordionContextMockBrowser } from 'auto-core/react/components/common/Accordion/AccordionContext.mock';
import ActionQueue from 'auto-core/react/components/common/ActionQueue/ActionQueue';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import { offerFormPageContextMockBrowser } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormPageContextProvider } from 'www-poffer/react/contexts/offerFormPage';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';

interface Props {
    children: React.ReactElement;
    initialValues?: FormValues<OfferFormFieldNamesType, OfferFormFields>;
    state?: Partial<AppState>;
    coreContext?: typeof contextMockBrowser;
    offerFormContext?: typeof offerFormPageContextMockBrowser;
    statActionQueueApi?: React.RefObject<ActionQueueContext>;
    accordionContext?: typeof AccordionContextMockBrowser;
    formApi?: React.RefObject<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;
}

export const OfferFormComponentTestWrapper = ({
    accordionContext,
    children,
    coreContext,
    formApi,
    initialValues,
    offerFormContext,
    statActionQueueApi,
    state: stateFromProps,
}: Props) => {
    const formApiDefault = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const statActionQueueApiDefault = React.createRef<ActionQueueContext>();
    const state = {
        offerDraft: offerDraftMock.value(),
        ...stateFromProps,
    };

    const ContextProvider = createContextProvider(coreContext || contextMockBrowser);
    const store = mockStore(state || {});

    return (
        <ContextProvider>
            <Provider store={ store }>
                <OfferFormPageContextProvider value={ offerFormContext || offerFormPageContextMockBrowser }>
                    <AccordionContext.Provider value={ accordionContext || AccordionContextMockBrowser }>
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

export const renderComponent = (content: JSX.Element, props?: Omit<Props, 'children'>, options?: JestPuppeteerReactRenderOptions) => {
    return render(
        <OfferFormComponentTestWrapper { ...props }>
            { content }
        </OfferFormComponentTestWrapper>, options);
};
