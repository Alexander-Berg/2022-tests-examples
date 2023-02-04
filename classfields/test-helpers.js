import PropTypes from 'prop-types';
import React from 'react';
import { Provider } from 'react-redux';
import { createStore, applyMiddleware, compose, combineReducers } from 'redux';
import thunkMiddleware from 'redux-thunk';
import { createEpicMiddleware, createStateStreamEnhancer, combineEpics } from 'redux-most';
import { empty } from 'most';
import { browserHistory } from 'react-router-susanin';
import { routerMiddleware } from 'react-router-redux';
import merge from 'lodash/merge';

import legacyContext from '@realty-front/jest-utils/puppeteer/tests-helpers/legacy-context';

import OriginalGate from 'realty-core/view/react/libs/gate';
import configReducer from 'realty-core/view/react/common/reducers/config';
import userReducer from 'realty-core/view/react/common/reducers/user';
import cookiesReducer from 'realty-core/view/react/common/reducers/cookies';
import pageReducer from 'realty-core/view/react/common/reducers/page';
import seoReducer from 'realty-core/view/react/common/reducers/seo';
import geoReducer from 'realty-core/view/react/common/reducers/geo';
import routingReducer from 'realty-core/view/react/common/reducers/routing';

import { initializeStats } from 'realty-core/view/react/modules/metrics/stats/statsUtil';

import { initializeClientFeatureToggles } from 'realty-core/view/common/libs/feature-toggles';

const DEFAULT_EPIC = combineEpics([ () => empty() ]);
const DEFAULT_INITIAL_STATE = {
    config: {
        origin: 'https://realty.yandex.ru',
        domains: {
            desktop: 'https://realty.yandex.ru',
            'touch-phone': 'https://m.realty.yandex.ru'
        },
        adAliasName: 'biba',
        experimentsData: {}
    },
    routing: { locationBeforeTransitions: { pathname: '', search: '' } },
    user: { passportOrigin: 'https://passport.yandex.ru' },
    page: {},
    geo: { rgid: '587795' }
};

export const withContext = (options = {}) => {
    return {
        ...options,
        context: {
            link: () => {},
            scroll: () => {}
        },
        childContextTypes: {
            link: PropTypes.func,
            scroll: PropTypes.func,
            api: PropTypes.object
        }
    };
};

class StoreProvider extends React.Component {
    constructor(props) {
        super(props);
        const { store } = this.props;

        initializeStats(store);
    }

    render() {
        return <Provider {...this.props} />;
    }
}

export const withStoreProvider = (component, store) => {
    return (
        <StoreProvider store={store}>
            {component}
        </StoreProvider>
    );
};

export const createRootReducer = appendReducers => combineReducers({
    config: configReducer,
    user: userReducer,
    seo: seoReducer,
    geo: geoReducer,
    cookies: cookiesReducer,
    page: pageReducer,
    routing: routingReducer,
    ...appendReducers
});

export const proxyGate = Gate => new Proxy(Gate, {
    get(target, prop, receiver) {
        const fn = Reflect.get(target, prop, receiver);

        return function(path, params = {}) {
            window.__GatesHistory = window.__GatesHistory || [];

            window.__GatesHistory.push({
                type: 'pending',
                path,
                params
            });

            return fn.apply(this, arguments).then(response => {
                window.__GatesHistory.push({
                    type: 'success',
                    path,
                    response
                });

                return Promise.resolve(response);
            }).catch(e => {
                window.__GatesHistory.push({
                    type: 'error',
                    path,
                    response: e && e.toString()
                });

                return Promise.reject(e);
            });
        };
    }
});

export const AppProvider = ({
    rootReducer = state => state,
    initialState = DEFAULT_INITIAL_STATE,
    context,
    children,
    Gate = OriginalGate,
    rootEpic = DEFAULT_EPIC,
    disableSetTimeoutDelay = false,
    experiments,
    fakeTimers = null
}) => {
    Gate = proxyGate(Gate);

    window.experiments = experiments;

    initializeClientFeatureToggles(initialState);

    if (fakeTimers) {
        const config = typeof fakeTimers === 'object' ? fakeTimers : undefined;

        if (window.__clock) {
            window.__clock.uninstall();
        }

        window.__clock = window.FakeTimers.install(config);
    }

    const epicMiddleware = createEpicMiddleware(rootEpic);
    const store = createStore(
        rootReducer,
        merge(DEFAULT_INITIAL_STATE, initialState),
        compose(
            applyMiddleware(
                thunkMiddleware.withExtraArgument({ Gate }),
                routerMiddleware(browserHistory)
            ),
            createStateStreamEnhancer(epicMiddleware),
            window.__REDUX_DEVTOOLS_EXTENSION__ ? window.__REDUX_DEVTOOLS_EXTENSION__() : f => f
        )
    );

    if (disableSetTimeoutDelay) {
        const originalSetTimeoutFn = window.setTimeout;

        window.setTimeout = fn => originalSetTimeoutFn.apply(window, [ fn, 0 ]);
    }

    const LegacyContext = legacyContext({
        link: () => '',
        scroll: () => {},
        toggleScroll: () => {},
        navigate: () => {},
        router: {
            params: {},
            createHref: () => {},
            push: () => {},
            replace: () => {},
            go: () => {},
            goBack: () => {},
            goForward: () => {},
            setRouteLeaveHook: () => {},
            getCurrentLocation: () => ({}),
            isActive: () => false,
            location: {
                pathname: 'pathname',
                search: '',
                query: {},
                hash: ''
            }
        },
        ...context
    });

    return (
        <StoreProvider store={store}>
            <LegacyContext>
                {children}
            </LegacyContext>
        </StoreProvider>
    );
};

export const resolvePromise = (value, timeout, callback = () => {}) => {
    if (! timeout) {
        return Promise.resolve(value);
    }

    return new Promise(resolve =>
        setTimeout(
            () => {
                callback(value);
                resolve(value);
            },
            timeout
        )
    );
};

export const rejectPromise = (value, timeout, callback = () => {}) => {
    if (! timeout) {
        return Promise.reject(value);
    }

    return new Promise((resolve, reject) =>
        setTimeout(
            () => {
                callback(value);
                reject(value);
            },
            timeout
        )
    );
};

export const infinitePromise = () => new Promise(() => {});
