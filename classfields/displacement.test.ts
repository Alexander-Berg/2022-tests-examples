import displacement from './displacement';

it('должен вернуть ничто, если при округлении получается 0', () => {
    expect(displacement.get(3, 'GASOLINE')).toBeUndefined();
});

it('должен вернуть объем, если при округлении получается не 0', () => {
    expect(displacement.get(300, 'GASOLINE')).toBe('0.3');
});

describe('minmax', () => {
    it('вернёт ничто при пустом массиве', () => {
        expect(displacement.getMinMax([])).toBe(undefined);
    });
    it('при одном значении', () => {
        expect(displacement.getMinMax([ 3000 ])).toBe('3.0 л');
    });
    it('при двух значениях', () => {
        expect(displacement.getMinMax([ 3000, 4000 ])).toBe('3.0–4.0 л');
    });
    it('при двух одинаковых значениях', () => {
        expect(displacement.getMinMax([ 3000, 3000 ])).toBe('3.0 л');
    });
});
