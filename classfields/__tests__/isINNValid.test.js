const { isINNValid } = require('realty-core/app/lib/isINNValid');

describe('isINNValid', () => {
    it('должен возвращать true для валидного ИНН', () => {
        const result = isINNValid('781714171469');

        expect(result).toEqual(true);
    });

    it('должен возвращать false для невалидного ИНН', () => {
        const result = isINNValid('6449013710');

        expect(result).toEqual(false);
    });
});
