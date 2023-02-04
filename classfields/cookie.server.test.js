/**
 * @jest-environment node
 */

const cookie = require('./cookie');

it('должен бросить ошибку при вызове cookie.get на сервере', () => {
    expect(() => cookie.get('foo')).toThrow();
});

it('должен бросить ошибку при вызове cookie.set на сервере', () => {
    expect(() => cookie.set('foo', 'barr')).toThrow();
});

it('должен бросить ошибку при вызове cookie.remove на сервере', () => {
    expect(() => cookie.remove('remove')).toThrow();
});
