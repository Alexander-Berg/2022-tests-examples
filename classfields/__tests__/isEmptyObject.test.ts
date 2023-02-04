import { isEmptyObject } from '../isEmptyObject';

describe('isEmptyObject', () => {
    it('Должен работать корректно с пустым объектом', () => {
        expect(isEmptyObject({})).toEqual(true);
    });

    it('Должен работать корректно c undefined ключами', () => {
        expect(isEmptyObject({ key1: undefined, key2: undefined })).toEqual(true);
    });

    it('Должен работать корректно c заполненными ключами', () => {
        expect(isEmptyObject({ key: 0 })).toEqual(false);
    });
});
