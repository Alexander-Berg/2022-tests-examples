import React from 'react';
import { applyMiddleware, compose, createStore as createReduxStore } from 'redux';
import { Provider } from 'react-redux';
import PropTypes from 'prop-types';
import thunkMiddleware from 'redux-thunk';
import { createMemoryHistory, createPath } from 'history';

import { AppContextProvider } from 'view/libs/context';
import rootReducer from 'view/store/reducers';
import { configureFela } from 'view/libs/configure-fela';
import { createRouteLocator, createRouter } from 'view/router';
import 'view/styles/index.css';

import { createAppStoreProxy, runAppStore } from './store';

const { FelaProvider } = configureFela();

function createContext(options = {}) {
    const routeLocator = createRouteLocator();

    const defaultRoute = { page: 'clients', params: {} };
    const locationEntries = options.router && options.router.entries || [ defaultRoute ];
    const history = createMemoryHistory({
        initialEntries: locationEntries.map(routeLocator.getLocationByDescriptor).map(createPath)
    });
    const router = createRouter(history, routeLocator);

    const storeProxy = createAppStoreProxy();

    const appContext = {
        router,
        store: storeProxy.store,
        gate: options.gate
    };

    runAppStore(storeProxy, appContext, options.store && options.store.initialState);

    return appContext;
}

export const AppProviders = ({ context, store, children }) => {
    const middlewares = [ thunkMiddleware ];
    const reduxStore = createReduxStore(rootReducer, store, compose(
        applyMiddleware(...middlewares)
    ));
    const appContext = createContext(context);

    return (
        <Provider store={reduxStore}>
            <AppContextProvider value={appContext}>
                <FelaProvider>
                    {children}
                </FelaProvider>
            </AppContextProvider>
        </Provider>
    );
};

AppProviders.propTypes = {
    // initial redux store
    store: PropTypes.any,
    // initial context state
    context: PropTypes.any
};
