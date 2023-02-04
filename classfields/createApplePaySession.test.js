/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return { getResource: jest.fn() };
});

require('autoru-frontend/mocks/applePaySessionMock');

const getResource = require('auto-core/react/lib/gateApi').getResource;
const createApplePaySession = require('./createApplePaySession').default;

beforeAll(() => {
    getResource.mockClear();
});

it('при создании отправляет запрос на валидацию продавца', () => {
    getResource.mockImplementation(() => Promise.resolve());
    createApplePaySession({});
    expect(getResource).toHaveBeenCalledWith('performApplePayValidation', { validationURL: 'hello!', displayName: 'Авто.ру', host: 'localhost' });
});

it('обрабатывает ошибку валидации продавца', () => {
    getResource.mockImplementation(() => Promise.reject());

    const params = {
        onValidationError: jest.fn(),
        onAuthorizationComplete: jest.fn(),
    };
    createApplePaySession(params);

    // Надо дождаться, чтобы все колбеки отработали
    return new Promise((resolve) => {
        setTimeout(() => {
            expect(params.onValidationError).toHaveBeenCalled();
            resolve();
        }, 0);
    });
});

it('на авторизацию платежа вызовет onAuthorizationComplete и передаст туда токен', () => {
    getResource.mockImplementation(() => Promise.resolve());

    const onAuthorizationComplete = jest.fn().mockImplementation(() => Promise.resolve([]));
    const token = 'dW5kZWZpbmVk'; // base64
    const session = createApplePaySession({ onAuthorizationComplete });
    session.onpaymentauthorized({ payment: { token } });

    expect(onAuthorizationComplete).toHaveBeenCalledWith(token);
});
