import React from 'react';
import { Epic } from 'redux-most';
import { Reducer, Action } from 'redux';

import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { AnyObject } from 'realty-core/types/utils';

export interface IAppProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    rootReducer?: Reducer<any>;
    initialState?: AnyObject;
    context?: AnyObject;
    children?: React.ReactNode;
    Gate?: AnyObject;
    rootEpic?: Epic<{ type: unknown }, Action<unknown>>;
    disableSetTimeoutDelay?: boolean;
    experiments?: string[];
    fakeTimers?: AnyObject | true;
}

declare const AppProvider: React.FC<IAppProviderProps>;

declare const createRootReducer: <T>(appendReducers: T) => Reducer<T & ICoreStore>;

declare const proxyGate: (Gate: AnyObject) => AnyObject;

declare const resolvePromise: <T>(value?: T, timeout?: number, callback?: (value: T) => void) => Promise<T>;
declare const rejectPromise: <T>(value?: T, timeout?: number, callback?: (value: T) => void) => Promise<T>;
declare const infinitePromise: () => Promise<void>;
