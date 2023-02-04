const formatNumKilo = require('./number-kilo-formatter');
it('должен вернуть число без форматирования, если количество цифр меньше <=3', () => {
    expect(formatNumKilo(455)).toBe(455);
});

it('должен вернуть число 99+К, если количество цифр >=5', () => {
    expect(formatNumKilo(99001)).toBe('99K+');
});

it('должен вернуть число с суффиксом К', () => {
    expect(formatNumKilo(1005)).toBe('1K');
});

it('должен вернуть число с суффиксом К, если указан precision', () => {
    expect(formatNumKilo(1050, 2)).toBe('1.05K');
});
