/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { SUBSCRIPTION_FORM_ID } from '../../SubscriptionForm/constants';

import isTelegramOrSubscriptionForm from './isTelegramOrSubscriptionForm';

it('возвращает "telegram" если позиция баннера в блоке', () => {
    const result = isTelegramOrSubscriptionForm({
        position: 'block',
        subscribed: false,
        type: 'form',
    });

    expect(result).toBe('telegram');
});

it('возвращает "telegram" если пользователь уже подписался', () => {
    const result = isTelegramOrSubscriptionForm({
        position: 'footer',
        subscribed: true,
        type: 'form',
    });

    expect(result).toBe('telegram');
});

it('возвращает "telegram" если отключена форма подписки', () => {
    const result = isTelegramOrSubscriptionForm({
        position: 'footer',
        subscribed: false,
        type: 'form',
    }, true);

    expect(result).toBe('telegram');
});

it('возвращает "form" если position="footer" и type="form"', () => {
    expect(isTelegramOrSubscriptionForm({
        position: 'footer',
        subscribed: false,
        type: 'form',
    })).toBe('form');
});

it('возвращает "telegram" если position="footer" и type="telegram"', () => {
    expect(isTelegramOrSubscriptionForm({
        position: 'footer',
        subscribed: false,
        type: 'telegram',
    })).toBe('telegram');
});

describe('якорь', () => {
    beforeEach(() => {
        window.location.hash = '#' + SUBSCRIPTION_FORM_ID;
    });

    afterEach(() => {
        window.location.hash = '';
    });

    it('возвращает "telegram" игнорируя якорь, если позиция баннера в блоке', () => {
        expect(isTelegramOrSubscriptionForm({
            position: 'block',
            subscribed: false,
            type: 'form',
        })).toBe('telegram');
    });

    it('возвращает "form" если позиция баннера в футере', () => {
        expect(isTelegramOrSubscriptionForm({
            position: 'footer',
            subscribed: false,
            type: 'telegram',
        })).toBe('form');
    });
});
