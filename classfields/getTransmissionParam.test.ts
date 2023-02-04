import getTransmissionParam from './getTransmissionParam';

describe('getListingCanonicalUrl', () => {

    it('должен вернуть результат в виде строки, если аргумент функции не массив', () => {
        expect(getTransmissionParam('AUTO')).toEqual('AUTO');
    });

    it('должен вернуть результат в виде строки, если аргумент функции массив', () => {
        expect(getTransmissionParam([ 'VARIATOR' ])).toEqual('VARIATOR');
    });

    it('должен вернуть AUTO, если трансмиссия автоматическая', () => {
        expect(getTransmissionParam([ 'AUTOMATIC' ])).toEqual('AUTO');
    });

    it('возвращает null, если аргумент пустая строка', () => {
        expect(getTransmissionParam('')).toBeNull();
    });

});
