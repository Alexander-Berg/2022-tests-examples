/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import isTelegramOrSubscriptionForm from './isTelegramOrSubscriptionForm';

it('возвращает ответ, если есть кука', () => {
    const result = isTelegramOrSubscriptionForm({
        uniqBlockTypes: [ 'autoSubscription' ],
        cookies: {
            subscribedFormToPosts: '1',
        },
        subscribeOff: false,
    });

    expect(result).toMatchSnapshot();
});

it('возвращает ответ, если отключена форма подписки', () => {
    const result = isTelegramOrSubscriptionForm({
        uniqBlockTypes: [ 'autoSubscription' ],
        cookies: {},
        subscribeOff: true,
    });

    expect(result).toMatchSnapshot();
});

it('возвращает ответ, если есть блок с формой подписки', () => {
    const result = isTelegramOrSubscriptionForm({
        uniqBlockTypes: [ 'autoSubscription' ],
        cookies: {},
        subscribeOff: false,
    });

    expect(result).toMatchSnapshot();
});

describe('рандом', () => {
    beforeEach(() => {
        jest.spyOn(global.Math, 'random').mockRestore();
    });

    it('возвращает position=footer, type=form, если рандом вернул < 0.5', () => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.3193972778521954);

        const result = isTelegramOrSubscriptionForm({
            uniqBlockTypes: [],
            cookies: {},
            subscribeOff: false,
        });

        expect(result.position).toBe('footer');
        expect(result.type).toBe('form');
    });

    it('возвращает position=footer, type=telegram, если рандом вернул > 0.5', () => {
        jest.spyOn(global.Math, 'random').mockReturnValue(0.8165748523369281);

        const result = isTelegramOrSubscriptionForm({
            uniqBlockTypes: [],
            cookies: {},
            subscribeOff: false,
        });

        expect(result.position).toBe('footer');
        expect(result.type).toBe('telegram');
    });
});
