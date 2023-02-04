import isPhoneValid from './isPhoneValid';

it('возвращает false для номера, где вторая цифра 7, как в случае с PhoneInput', () => {
    expect(isPhoneValid('7711111111')).toBe(false);
});

it('возвращает true для валидного номера', () => {
    expect(isPhoneValid('78111111111')).toBe(true);
});
