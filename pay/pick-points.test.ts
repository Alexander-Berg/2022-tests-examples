import * as Sdk from '@yandex-pay/sdk/src/typings';

import { isPointInBounds } from './pick-points';

describe('isPointInBounds', function () {
    const rect: Sdk.GeoBounds = {
        ne: {
            latitude: 0,
            longitude: 0,
        },
        sw: {
            latitude: 10,
            longitude: 10,
        },
    };

    it('should return true if in box', function () {
        expect(isPointInBounds(rect, { latitude: 5, longitude: 5 })).toBeTruthy();
    });

    it('should return false if not in box', function () {
        expect(isPointInBounds(rect, { latitude: 20, longitude: 20 })).toBeFalsy();
    });

    it('should return true if on border', function () {
        expect(isPointInBounds(rect, { latitude: 0, longitude: 0 })).toBeTruthy();
        expect(isPointInBounds(rect, { latitude: 10, longitude: 10 })).toBeTruthy();
    });
});
