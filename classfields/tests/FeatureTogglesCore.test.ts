import { FeatureTogglesCore } from '../FeatureTogglesCore';
import { Any } from '../types';

import { defaultFeatureTogglesConfig, getDeferredSource } from './utils';

describe('FeatureTogglesCore', () => {
    test('connection retries', async () => {
        const { source, list } = getDeferredSource();
        const customConfig = {
            ...defaultFeatureTogglesConfig,
            connection: {
                ...defaultFeatureTogglesConfig.connection,
                retries: 2,
                retryDelay: 200,
            },
        };

        const featureToggles = new FeatureTogglesCore(source, customConfig);
        const spyList = jest.spyOn(source as Any, 'list');

        const initPromise = featureToggles.list();

        list.reject();

        await initPromise;

        await new Promise((resolve) => setTimeout(resolve, customConfig.connection.retryDelay * 2));

        const firstRetryPromise = featureToggles.list();

        list.reject();

        await firstRetryPromise;

        await new Promise((resolve) => setTimeout(resolve, customConfig.connection.retryDelay * 2));

        const secondRetryPromise = featureToggles.list();

        list.reject();

        await secondRetryPromise;

        expect(spyList).toBeCalledTimes(3);
    });

    test('get last cached value when error happened', async () => {
        const { source, list } = getDeferredSource();
        const customConfig = {
            ...defaultFeatureTogglesConfig,
            connection: {
                ...defaultFeatureTogglesConfig.connection,
                retries: 2,
                retryDelay: 200,
            },
        };

        const featureToggles = new FeatureTogglesCore(source, customConfig);

        const initPromise = featureToggles.list();

        const firstFeature = { key: 'FEATURE_1', value: 'F1' };
        const secondFeature = { key: 'FEATURE_1', value: 'F1' };

        list.resolve([firstFeature, secondFeature]);

        await initPromise;

        await new Promise((resolve) => setTimeout(resolve, customConfig.refresh.timeout));

        const secondPromise = featureToggles.get('FEATURE_1');

        list.reject();

        expect(await secondPromise).toEqual(firstFeature);
    });
});
