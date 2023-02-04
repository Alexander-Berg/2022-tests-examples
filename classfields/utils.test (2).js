const { getSearchList } = require('./utils');

describe('getSearchList', () => {
    it('группирует элементы по буквам', () => {
        const params = {
            entities: [
                { name: 'Alfa Romeo' },
                { name: 'aston Martin' },
                { name: 'Cadillac' },
            ],
            fieldName: 'mark',
        };

        expect(getSearchList(params)).toEqual([
            { key: 'A', list: [ { name: 'Alfa Romeo' }, { name: 'aston Martin' } ] },
            { key: 'C', list: [ { name: 'Cadillac' } ] },
        ]);
    });

    it('если передано поле для фильтрации, оставит только элементы с count > 0 и значением из параметров страницы', () => {
        const params = {
            entities: [
                { name: 'Alfa Romeo', reviews_count: 42 },
                { name: 'Aston Martin', reviews_count: 0 },
                { name: 'Cadillac', reviews_count: 15 },
                { name: 'Dodge', reviews_count: 0, id: 'DODGE' },
            ],
            filterField: 'reviews_count',
            fieldName: 'mark',
            valueFromParams: 'DODGE',
        };

        expect(getSearchList(params)).toEqual([
            { key: 'A', list: [ { name: 'Alfa Romeo', reviews_count: 42 } ] },
            { key: 'C', list: [ { name: 'Cadillac', reviews_count: 15 } ] },
            { key: 'D', list: [ { name: 'Dodge', reviews_count: 0, id: 'DODGE' } ] },
        ]);
    });

    it('если передана строка поиска, оставит только то что совпадает со строкой', () => {
        const params = {
            entities: [
                { name: 'Borrego', reviews_count: 0, id: 'BORREGO' },
                { name: 'Capital', reviews_count: 15 },
                { name: 'Carnival', reviews_count: 42 },
                { name: 'Carstar', reviews_count: 0 },
            ],
            filterField: 'reviews_count',
            fieldName: 'model',
            valueFromParams: 'BORREGO',
            searchTerm: 'car',
        };

        expect(getSearchList(params)).toEqual([
            { key: 'C', list: [ { name: 'Carnival', reviews_count: 42 } ] },
        ]);
    });

    it('если в марках ничего не нашел по переданной строке, то отдаст пустой массив', () => {
        const params = {
            entities: [
                { name: 'Alfa Romeo', reviews_count: 42 },
                { name: 'Aston Martin', reviews_count: 0 },
                { name: 'Cadillac', reviews_count: 15 },
                { name: 'Dodge', reviews_count: 0, id: 'DODGE' },
            ],
            filterField: 'reviews_count',
            fieldName: 'mark',
            valueFromParams: 'DODGE',
            searchTerm: 'alpi',
        };

        expect(getSearchList(params)).toEqual([]);
    });

    it('в моделях если ничего не нашел в имени, попробует найти в неймплейтах и отдаст результат в другом формате', () => {
        const params = {
            entities: [
                { name: 'Rio', id: 'RIO', reviews_count: 15, nameplates: [
                    { name: 'Rio' },
                    { name: 'X' },
                    { name: 'X-Line' },
                ] },
                { name: 'Picanto', id: 'PICANTO', reviews_count: 42, nameplates: [
                    { name: 'Picanto' },
                    { name: 'GT Line' },
                    { name: 'X-Line' },
                ] },
                { name: 'Ceed', id: 'CEED', reviews_count: 0, nameplates: [
                    { name: 'Ceed' },
                    { name: 'Proceed' },
                    { name: 'X-Line' },
                ] },
            ],
            filterField: 'reviews_count',
            fieldName: 'model',
            searchTerm: 'li',
        };

        expect(getSearchList(params)).toEqual([
            { fieldName: 'nameplate', model: 'RIO', name: 'Rio X-Line' },
            { fieldName: 'nameplate', model: 'PICANTO', name: 'Picanto GT Line' },
            { fieldName: 'nameplate', model: 'PICANTO', name: 'Picanto X-Line' },
        ]);
    });
});
