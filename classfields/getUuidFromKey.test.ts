import getUuidFromKey from './getUuidFromKey';

describe('валидные ключи', () => {
    it.each([
        [
            'CAGAAcu91YMGiAHL2YqLBpAB66Pc4YnO9q1cmAHts7bP6uSAqIQB',
            '5c5bda70-9c37-11eb-8450-0326a9ed99ed',
        ],
        [
            'CAGAAcvG1oMGiAHL4ouLBpAB66OA44ne2OQwmAH8vdH_7uvo6rUB',
            '30c962f0-9c60-11eb-b5d5-a35eeff45efc',
        ],
        [
            'CAGAAdm-kIQGiAHZ2sWLBpAB66OIqIqA1YJdmAHhl7yovauYqZYB',
            '5d055400-a502-11eb-9652-615bd50f0be1',
        ],
    ])('должен преобразовать ключ %s в uuid %s', (key, value) => {
        expect(getUuidFromKey(key)).toEqual(value);
    });
});

describe('невалидные ключи', () => {
    it.each([
        [ 'kIQGiAHZ2sWLBpAB66OIqIqA1YJdmAHhl7yovauYqZYB' ],
    ])('не должен распарсить %s и вернуть null', (key) => {
        expect(getUuidFromKey(key)).toBeNull();
    });
});
