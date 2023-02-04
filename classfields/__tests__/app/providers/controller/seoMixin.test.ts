import { getEmptyOutputPageParams } from 'realty-core/app/controller/mixin/helpers/getEmptyOutputPageParams';

describe('Переход на верхний уровень при пустой выдаче', () => {
    it('с доп фильтров на основные фильтры', () => {
        const params = getEmptyOutputPageParams({ rgid: 123, type: 'SELL', category: 'APARTMENT', roomsTotal: 3 });
        expect(params).toEqual({ rgid: 123, type: 'SELL', category: 'APARTMENT' });
    });

    it('с основных фильтров на главную', () => {
        const params = getEmptyOutputPageParams({ rgid: 123, type: 'SELL', category: 'APARTMENT' });
        expect(params).toEqual({});
    });
});
