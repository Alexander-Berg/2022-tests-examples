/* eslint-disable @typescript-eslint/no-unused-vars */
import noop from 'lodash';

import { ExtractFeatureByKey, GetKey, IBaseFeatureToggle, IFeatureToggleSource, PromiseResolveType } from '../types';

interface IDeferredValue extends IBaseFeatureToggle {}

export const getDeferredSource = () => {
    const list: { resolve: PromiseResolveType; reject: PromiseResolveType } = {
        resolve: noop,
        reject: noop,
    };

    class SourceTest implements IFeatureToggleSource<IDeferredValue> {
        list(): Promise<IDeferredValue[]> {
            return new Promise<IDeferredValue[]>((resolve, reject) => {
                list.resolve = resolve;
                list.reject = reject;
            });
        }

        delete(key: GetKey<IDeferredValue>): Promise<void> {
            return Promise.resolve(undefined);
        }

        set<K extends GetKey<IDeferredValue>>(key: K, value: ExtractFeatureByKey<K, IDeferredValue>): Promise<void> {
            return Promise.resolve(undefined);
        }
    }

    const source = new SourceTest();

    return { source, list };
};

export const defaultFeatureTogglesConfig = {
    refresh: {
        timeout: 500,
    },
    connection: {
        timeout: 2000,
        retries: 2,
        retryDelay: 3000,
    },
    policies: {
        onCacheUpdating: 'waitForNewValue',
        onConnectionTimeout: 'resolve',
    },
} as const;
