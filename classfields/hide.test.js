const hide = require('./hide');

it('должен вернуть корректный результат', () => {
    expect(hide()).toEqual({
        type: 'HIDE_BADGES_SETTINGS',
    });
});
