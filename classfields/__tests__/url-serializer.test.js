import { serializeParam } from '../';

describe('serializeParam', () => {
    it('returns an object with the commissionMax text', () => {
        const result = serializeParam({ name: 'commissionMax', value: 70 });

        expect(result.text).toBe('комиссия агенту не более 70%');
    });
});
