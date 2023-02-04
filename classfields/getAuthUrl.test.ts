import configMock from 'auto-core/react/dataDomain/config/mocks/config';

import getAuthUrl from './getAuthUrl';

const STATE = {
    config: configMock,
};

it('должен вернуть ссылку на авторизацию', () => {
    expect(getAuthUrl(STATE)).toBe('https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2F');
});

it('должен вернуть ссылку на авторизацию, если адрес страницы начинается без "/"', () => {
    expect(getAuthUrl(STATE, { authPath: 'auth/' })).toBe('https://autoru_frontend.auth_domain/auth/?r=https%3A%2F%2Fauto.ru%2F');
});

it('должен вернуть ссылку на авторизацию, если адрес страницы заканчивается без "/"', () => {
    expect(getAuthUrl(STATE, { authPath: '/auth' })).toBe('https://autoru_frontend.auth_domain/auth/?r=https%3A%2F%2Fauto.ru%2F');
});

it('должен вернуть ссылку на авторизацию, если кстомный returnPath', () => {
    expect(getAuthUrl(STATE, { returnPath: '/xxx/' })).toBe('https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2Fxxx%2F');
});
