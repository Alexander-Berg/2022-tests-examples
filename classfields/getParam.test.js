const getParam = require('./getParam');

describe('getListingCanonicalUrl', () => {

    it('должен вернуть результат в виде строки, если аргумент функции не массив', () => {
        expect(getParam('FAFBFB')).toEqual('FAFBFB');
    });

    it('должен вернуть результат в виде строки, если аргумент функции массив', () => {
        expect(getParam([ 'REAR_DRIVE' ])).toEqual('REAR_DRIVE');
    });

    it('возвращает null, если аргумент пустая строка', () => {
        expect(getParam('')).toBeNull();
    });

});
