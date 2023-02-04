const formatFilterPhone = require('./formatFilterPhone');

it('должен удалять буквы и другие недопустимые символы из телефона', () => {
    expect(formatFilterPhone('ad7340^&!990')).toBe('7340990');
});

it('не должен удалять допустимые символы и цифры из телефона', () => {
    expect(formatFilterPhone('+7(999) 987-02-33')).toBe('+7(999) 987-02-33');
});
