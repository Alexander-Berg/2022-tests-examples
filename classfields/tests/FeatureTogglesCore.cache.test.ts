import { delay } from 'lodash';

import { FeatureTogglesCore } from '../FeatureTogglesCore';
import { Any } from '../types';

import { defaultFeatureTogglesConfig, getDeferredSource } from './utils';

describe('FeatureTogglesCore cache', () => {
    test('cache is warmed after success source result', async () => {
        const { source, list } = getDeferredSource();

        const featureToggles = new FeatureTogglesCore(source, defaultFeatureTogglesConfig);

        const promise = featureToggles.list();

        expect((featureToggles as Any).cache.isWarmed()).toBeFalsy();

        list.resolve([]);

        await promise;

        expect((featureToggles as Any).cache.isWarmed()).toBeTruthy();
    });

    test('cache is synchronized with source updates', async () => {
        const { source, list } = getDeferredSource();
        const customConfig = {
            ...defaultFeatureTogglesConfig,
            refresh: {
                timeout: 100,
            },
        };

        const featureToggles = new FeatureTogglesCore(source, customConfig);

        const initialListPromise = featureToggles.list();
        const feature1 = { key: 'FEATURE_1', value: 'F1' };
        const feature2 = { key: 'FEATURE_2', value: 'F2' };
        const feature3 = { key: 'FEATURE_3', value: 'F3' };

        // initial source data
        list.resolve([feature1]);

        expect(await initialListPromise).toEqual([feature1]);
        expect((featureToggles as Any).cache.list()).toEqual([feature1]);

        // adding features in source
        delay(async () => {
            const secondListPromise = featureToggles.list();

            list.resolve([feature1, feature2, feature3]);

            expect(await secondListPromise).toEqual([feature1, feature2, feature3]);
            expect((featureToggles as Any).cache.list()).toEqual([feature1, feature2, feature3]);
            expect(await featureToggles.get('FEATURE_2')).toEqual(feature2);

            // deleting features in source
            delay(async () => {
                const secondListPromise = featureToggles.list();

                list.resolve([feature3]);

                expect(await secondListPromise).toEqual([feature3]);
                expect((featureToggles as Any).cache.list()).toEqual([feature3]);
                expect(await featureToggles.get('FEATURE_2')).toEqual(undefined);
                expect(await featureToggles.get('FEATURE_3')).toEqual(feature3);
            }, customConfig.refresh.timeout);
        }, customConfig.refresh.timeout);
    });
});
