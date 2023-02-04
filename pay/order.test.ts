jest.mock('utils/random-string');

import { getOrderId } from './order';

describe('getOrderId', function () {
    it('should return memoized order', () => {
        expect(getOrderId('test-id', 1)).toBe(getOrderId('test-id', 1));
        expect(getOrderId('test-id', 2)).toBe(getOrderId('test-id', 2));
        expect(getOrderId('test-id', 3)).toBe(getOrderId('test-id', 3));
        expect(getOrderId('test-id', 3)).toBe(getOrderId('test-id', 3));
    });
});
