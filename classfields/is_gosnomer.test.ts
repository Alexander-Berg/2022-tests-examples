import is_gosnomer from './is_gosnomer';

describe('вернет true', () => {
    it('для корректного номера формата a111aa99', () => {
        expect(is_gosnomer('a123ok77')).toBe(true);
    });

    it('для корректного номера формата aa11199', () => {
        expect(is_gosnomer('ве11199')).toBe(true);
    });

    it('для корректного номера формата a111999', () => {
        expect(is_gosnomer('м111999')).toBe(true);
    });

    it('для корректного номера формата 1111aa99', () => {
        expect(is_gosnomer('1111ек99')).toBe(true);
    });

    it('для корректного номера формата a1111aa', () => {
        expect(is_gosnomer('о1111рх')).toBe(true);
    });

    it('для корректного номера формата aa111a99', () => {
        expect(is_gosnomer('ст111у99')).toBe(true);
    });

    it('для корректного номера формата aa1234567', () => {
        expect(is_gosnomer('ао1234567')).toBe(true);
    });

    it('для корректного номера внутри текста', () => {
        expect(is_gosnomer('посмотри какой номерок a111aa99!?! блатной')).toBe(true);
    });
});

describe('вернет false', () => {
    it('если есть неправильные символы', () => {
        expect(is_gosnomer('a123qk77')).toBe(false);
    });
});
