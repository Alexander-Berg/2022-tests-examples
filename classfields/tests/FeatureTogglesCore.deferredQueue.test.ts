import { delay } from 'lodash';

import { FeatureTogglesCore } from '../FeatureTogglesCore';
import { Any } from '../types';

import { defaultFeatureTogglesConfig, getDeferredSource } from './utils';

describe('FeatureTogglesCore deferred queue', () => {
    // eslint-disable-next-line max-len
    test('deferred requests are added while an initialization and resolved in correct order after a source response', async () => {
        const { source, list } = getDeferredSource();

        const featureToggles = new FeatureTogglesCore(source, defaultFeatureTogglesConfig);
        const promisesOrder: number[] = [];

        // eslint-disable-next-line jest/valid-expect-in-promise
        const promises = [
            featureToggles.list().then(() => promisesOrder.push(1)),
            featureToggles.get('FEATURE_1').then(() => promisesOrder.push(2)),
            featureToggles.get('FEATURE_2').then(() => promisesOrder.push(3)),
            featureToggles.list().then(() => promisesOrder.push(4)),
        ];

        expect((featureToggles as Any).deferredQueue).toHaveLength(4);

        list.resolve([]);

        await Promise.all(promises);

        expect((featureToggles as Any).deferredQueue).toHaveLength(0);
        expect(promisesOrder).toEqual([1, 2, 3, 4]);
    });

    test('deferred requests are collecting while updating with waitForNewValue policy', async () => {
        const { source, list } = getDeferredSource();
        const featureToggles = new FeatureTogglesCore(source, defaultFeatureTogglesConfig);

        const firstPromise = featureToggles.list();

        list.resolve([]);

        await firstPromise;

        delay(async () => {
            const secondPromise = featureToggles.list();

            expect((featureToggles as Any).deferredQueue).toHaveLength(1);

            list.resolve([]);

            await secondPromise;

            expect((featureToggles as Any).deferredQueue).toHaveLength(0);
        }, defaultFeatureTogglesConfig.refresh.timeout);
    });

    test('deferred requests are getting last cached value while updating with getLastCachedValue policy', async () => {
        const { source, list } = getDeferredSource();
        const featureToggles = new FeatureTogglesCore(source, {
            ...defaultFeatureTogglesConfig,
            policies: {
                ...defaultFeatureTogglesConfig.policies,
                onCacheUpdating: 'getLastCachedValue',
            },
        });

        const firstPromise = featureToggles.list();

        list.resolve([]);

        await firstPromise;

        delay(async () => {
            featureToggles.list();

            expect((featureToggles as Any).deferredQueue).toHaveLength(0);
        }, defaultFeatureTogglesConfig.refresh.timeout);
    });

    test('deferred requests are deleted if timeout take place', async () => {
        const { source } = getDeferredSource();
        const featureToggles = new FeatureTogglesCore(source, defaultFeatureTogglesConfig);

        featureToggles.list();
        featureToggles.get('FEATURE_1');

        expect((featureToggles as Any).deferredQueue).toHaveLength(2);

        delay(() => {
            expect((featureToggles as Any).deferredQueue).toHaveLength(0);
        }, defaultFeatureTogglesConfig.connection.timeout);
    });
});
