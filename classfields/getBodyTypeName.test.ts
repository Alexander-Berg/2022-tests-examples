import getBodyTypeName from './getBodyTypeName';

describe('getBodyTypeName', () => {
    it('вернет пустую строку так как был передан неизвестный тип кузова', () => {
        const actual = getBodyTypeName('XXX');

        expect(actual).toBe('');
    });

    it('вернет "Внедорожник 5 дв." если был передан тип "ALLROAD_5_DOORS"', () => {
        const actual = getBodyTypeName('ALLROAD_5_DOORS');

        expect(actual).toBe('Внедорожник 5 дв.');
    });
});
