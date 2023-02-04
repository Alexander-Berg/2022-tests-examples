import wrapFieldValue from './wrapFieldValue';

describe('wrapFieldValue превращает плоское значение в объект с полем value', () => {
    it('возвращает объект с полем value и переданным значением', () => {
        expect(wrapFieldValue(1)).toEqual({
            value: 1,
        });
    });

    it('возвращает объект с полем value и пустым значением, если значение равно undefined', () => {
        expect(wrapFieldValue(undefined)).toEqual({
            value: '',
        });
    });
});
