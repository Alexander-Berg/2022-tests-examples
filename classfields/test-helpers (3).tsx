import React from 'react';

import { AppProvider as OriginalAppProvider, IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';

export const AppProvider: React.FC<IAppProviderProps> = ({
    rootReducer,
    initialState,
    context,
    children,
    Gate,
    rootEpic,
    fakeTimers,
} = {}) => (
    <OriginalAppProvider
        rootReducer={rootReducer}
        initialState={initialState}
        context={context}
        Gate={Gate}
        rootEpic={rootEpic}
        fakeTimers={fakeTimers}
    >
        {children}
    </OriginalAppProvider>
);
