const emailValidator = require('./email-validator');

it('должен вернуть true, если email - валидный', () => {
    [
        'test@example.com',
        'test.test@example.com',
    ].forEach((email) => {
        expect(emailValidator(email)).toBe(true);
    });
});

it('должен вернуть false, если email не прошел валидацию', () => {
    [
        'test@example.com <egor>',
        'test.ru',
    ].forEach((email) => {
        expect(emailValidator(email)).toBe(false);
    });
});
