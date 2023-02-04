// eslint-disable-next-line no-redeclare
/* global jest */
import PropTypes from 'prop-types';
import React from 'react';
import { Provider } from 'react-redux';

import { AppProvider as OriginalAppProvider } from 'realty-core/view/react/libs/test-helpers';

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

export const withStoreProvider = (component, store) => {
    return (
        <Provider store={store}>
            {component}
        </Provider>
    );
};

export const AppProvider = props => <OriginalAppProvider {...props} />;

export const resolvePromise = (value, timeout, callback = () => {}) => {
    if (! timeout) {
        return () => Promise.resolve(value);
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
        return () => Promise.reject(value);
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

export const infinitePromise = () => () => new Promise(() => {});
