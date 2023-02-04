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
import type { AppStateCore } from 'auto-core/react/AppState';
import type { Fields, FieldNames } from 'auto-core/react/components/common/Form/fields/types';

interface Props {
    children: React.ReactElement;
    initialValues?: FormValues<FieldNames, Fields>;
    state?: Partial<AppStateCore>;
    coreContext?: typeof contextMockBrowser;
    statActionQueueApi?: React.RefObject<ActionQueueContext>;
    accordionContext?: typeof AccordionContextMockBrowser;
    formApi?: React.RefObject<FormContext<FieldNames, Fields, FieldErrors>>;
}

export const TestWrapper = ({
    accordionContext,
    children,
    coreContext,
    formApi,
    initialValues,
    statActionQueueApi,
    state,
}: Props) => {
    const formApiDefault = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const statActionQueueApiDefault = React.createRef<ActionQueueContext>();
    const ContextProvider = createContextProvider(coreContext || contextMockBrowser);
    const store = mockStore(state || {});

    return (
        <ContextProvider>
            <Provider store={ store }>
                <AccordionContext.Provider value={ accordionContext || AccordionContextMockBrowser }>
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

export const renderComponent = (content: JSX.Element, props?: Omit<Props, 'children'>, options?: JestPuppeteerReactRenderOptions) => {
    return render(
        <TestWrapper { ...props }>
            { content }
        </TestWrapper>, options);
};
