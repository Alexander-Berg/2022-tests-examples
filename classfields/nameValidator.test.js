const nameValidator = require('./nameValidator');

it('должен вернуть true, для строки из двух и более слов', () => {
    expect(nameValidator('раз два')).toBe(true);
    expect(nameValidator('раз два три четыре')).toBe(true);
});

it('должен вернуть false, для строки из одного слова', () => {
    expect(nameValidator('первое')).toBe(false);
    expect(nameValidator('первое ')).toBe(false);
});

it('должен вернуть false, при отсутствии входных данных', () => {
    expect(nameValidator()).toBe(false);
});

it('должен вернуть false, если на входе не строка', () => {
    expect(nameValidator(5)).toBe(false);
});
