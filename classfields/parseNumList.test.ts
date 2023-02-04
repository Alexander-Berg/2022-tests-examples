import { parseNumList } from './parseNumList';

describe('Парсинг тестовых id из строки', () => {
    it('Разделяет id по запятой', () => {
        expect(parseNumList('123')).toEqual([ '123' ]);
        expect(parseNumList('123,456')).toEqual([ '123', '456' ]);
        expect(parseNumList('123, 456')).toEqual([ '123', '456' ]);
    });

    it('Удаляет все нечисловые символы', () => {
        expect(parseNumList('123!#%qwrавып,4!#%qwrавып56')).toEqual([ '123', '456' ]);
    });

    it('Удаляет все пустые id, за исключением последнего', () => {
        expect(parseNumList('123, 456, , asd,')).toEqual([ '123', '456', '' ]);
    });
});
