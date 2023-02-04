const aliases = require('realty-router/lib/filters/aliases');
const t = require('realty-router/i18n/filters').ru;

describe('aliases', () => {
    it('Для каждого алиаса есть перевод', () => {
        Object.values(aliases).forEach(alias => {
            expect(Boolean(t[alias])).toBe(true);
        });
    });
});
