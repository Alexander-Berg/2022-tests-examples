const { formatCardMask } = require('./credit-card');

describe('function "formatCardMask"', () => {
    it('форматирует маску корректно, если она передана в формате без разделителя', () => {
        const result = formatCardMask('4444444448');
        expect(result).toBe('4444 44** **** 4448');
    });

    it('форматирует маску корректно, если она передана в формате с разделителем', () => {
        const result = formatCardMask('444444|4448');
        expect(result).toBe('4444 44** **** 4448');
    });
});
