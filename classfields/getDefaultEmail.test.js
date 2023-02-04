const getDefaultEmail = require('./getDefaultEmail');

const userMock = require('auto-core/react/dataDomain/user/mocks/dealerWithAccess.mock');

it('должен возвращать email клиента, если нет email у пользователя', () => {
    const expectedEmail = 'test@email.com';

    const state = {
        config: {
            client: {
                email: expectedEmail,
            },
        },
        user: { data: {} },
    };

    expect(getDefaultEmail(state)).toBe(expectedEmail);
});

it('должен возвращать email текущего юзера, если нет email клиента', () => {
    const state = {
        user: userMock,
    };

    expect(getDefaultEmail(state)).toBe('demo@auto.ru');
});
