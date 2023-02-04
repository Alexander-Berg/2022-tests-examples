const everyNDays = require('./everyNDays');

describe('function "everyNDays"', () => {
    it('returns correct text for 1 day', () => {
        const result = everyNDays(1);
        expect(result).toContain('день');
    });

    it('returns correct text for 2 days', () => {
        const result = everyNDays(2);
        expect(result).toContain('дня');
    });

    it('returns correct text for 5 days', () => {
        const result = everyNDays(5);
        expect(result).toContain('дней');
    });

    it('returns correct text for 21 days', () => {
        const result = everyNDays(21);
        expect(result).toContain('день');
    });
});
