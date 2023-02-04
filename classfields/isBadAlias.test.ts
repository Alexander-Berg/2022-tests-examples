import isBadAlias from './isBadAlias';

it('вернет true, если в нике есть айдишник', () => {
    expect(isBadAlias('id123321')).toBe(true);
});

it('вернет false, если в нике нет айдишника', () => {
    expect(isBadAlias('Normie')).toBe(false);
});
