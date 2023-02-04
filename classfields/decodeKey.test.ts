import decodeKey from './decodeKey';

describe('валидные ключи', () => {
    it.each([
        [
            'CAGAAcu91YMGiAHL2YqLBpAB66Pc4YnO9q1cmAHts7bP6uSAqIQB',
            { uuidMsb: '6655153051562086891', uuidLsb: '-8912620197971650067' },
        ],
        [
            'CAGAAcvG1oMGiAHL4ouLBpAB66OA44ne2OQwmAH8vdH_7uvo6rUB',
            { uuidMsb: '3515449769694597611', uuidLsb: '-5344185754657202436' },
        ],
        [
            'CAGAAdm-kIQGiAHZ2sWLBpAB66OIqIqA1YJdmAHhl7yovauYqZYB',
            { uuidMsb: '6702855982155960811', uuidLsb: '-7614916972885636127' },
        ],
    ])('должен распарсить %s', (key, value) => {
        const message = decodeKey(key);
        expect(message && message.toJSON()).toMatchObject(value);
    });
});

describe('невалидные ключи', () => {
    it.each([
        [ 'kIQGiAHZ2sWLBpAB66OIqIqA1YJdmAHhl7yovauYqZYB' ],
    ])('не должен распарсить %s и вернуть null', (key) => {
        expect(decodeKey(key)).toBeNull();
    });
});
