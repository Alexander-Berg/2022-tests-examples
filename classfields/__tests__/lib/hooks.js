const hooks = require('../../lib/hooks');

describe('hooks', function() {
    describe('translitSiteName', function() {
        it('Должен корректно преобразовывать название (начинается с z-)', function() {
            expect(hooks.translitSiteUrl('z-town')).toBe('ztown');
        });

        it('Должен корректно преобразовывать название', function() {
            expect(hooks.translitSiteUrl('sitename')).toBe('sitename');
        });
    });
});
