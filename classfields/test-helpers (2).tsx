import React from 'react';
import PropTypes from 'prop-types';
import noop from 'lodash/noop';
import { createStore } from 'redux';

import { AppProvider as OriginalAppProvider } from 'realty-core/view/react/libs/test-helpers';
import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject, FunctionReturnAny } from 'realty-core/types/utils';

import { rootReducer as reducer } from 'view/common/reducers';

export const locationMock = {
    pathname: 'pathname',
    search: '',
    query: {},
    hash: '',
};

export const routerMock = {
    createHref: noop,
    push: noop,
    replace: noop,
    go: noop,
    goBack: noop,
    goForward: noop,
    getCurrentLocation: () => locationMock,
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    listen: () => () => {},
    setRouteLeaveHook: noop,
    isActive: noop,

    location: locationMock,
    params: {},
};

export const routerProps = {
    router: routerMock,
    location: locationMock,
    params: {},
    routes: [],
};

export const loginPropsMock = {
    buildLoginUrl: () => '',
    retpath: '',
    url: '',
};

export const withContext: <S>(
    method: FunctionReturnAny<[React.Component, AnyObject]>,
    component: React.Component,
    state?: S
) => ReturnType<typeof method> = (method, component, state) => {
    const options = {
        context: {
            store: createStore(reducer, state || {}),
            link: noop,
            scroll: noop,
            router: routerMock,
        },
        childContextTypes: {
            store: PropTypes.object.isRequired,
            link: PropTypes.func,
            scroll: PropTypes.func,
            router: PropTypes.object,
        },
    };

    return method(component, options);
};

export function waitTick() {
    return new Promise((resolve) => process.nextTick(resolve)); /* eslint-disable-line no-undef */
}

export const AppProvider: React.FC<IAppProviderProps> = ({
    rootReducer = reducer,
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
