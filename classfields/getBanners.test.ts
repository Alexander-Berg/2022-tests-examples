import getBanners from './getBanners';

it('должен правильно вернуть список баннеров', () => {
    const result = getBanners({
        uniqBlockTypes: [],
        cookies: {},
        subscribeOff: false,
    });

    expect(result).toMatchObject({
        telegramOrSubscriptionForm: {},
    });
});
