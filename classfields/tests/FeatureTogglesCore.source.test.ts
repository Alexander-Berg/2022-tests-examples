import { FeatureTogglesCore } from '../FeatureTogglesCore';
import { Any } from '../types';

import { defaultFeatureTogglesConfig, getDeferredSource } from './utils';

describe('FeatureTogglesCore source', () => {
    test('source calls take places just in refreshes', async () => {
        const { source, list } = getDeferredSource();
        const featureToggles = new FeatureTogglesCore(source, defaultFeatureTogglesConfig);
        const spyList = jest.spyOn(source as Any, 'list');

        const promises = [
            featureToggles.list(),
            featureToggles.get('FEATURE_1'),
            featureToggles.get('FEATURE_2'),
            featureToggles.list(),
        ];

        list.resolve([]);

        await Promise.all(promises);

        expect(spyList).toBeCalledTimes(1);

        // internal timeout add 1 updateFeatureToggleCall and 1 source call
        await new Promise((resolve) => setTimeout(resolve, defaultFeatureTogglesConfig.refresh.timeout));

        const secondPromises = [featureToggles.list(), featureToggles.get('FEATURE_2'), featureToggles.list()];

        list.resolve([]);

        await Promise.all(secondPromises);

        expect(spyList).toBeCalledTimes(2);

        const thirdPromises = [featureToggles.list(), featureToggles.get('FEATURE_1'), featureToggles.list()];

        list.resolve([]);

        await Promise.all(thirdPromises);

        expect(spyList).toBeCalledTimes(2);

        // internal timeout add 1 updateFeatureToggleCall and 1 source call
        await new Promise((resolve) => setTimeout(resolve, defaultFeatureTogglesConfig.refresh.timeout * 2));

        const lastPromise = featureToggles.list();

        list.resolve([]);

        await lastPromise;

        expect(spyList).toBeCalledTimes(3);
    });
});
