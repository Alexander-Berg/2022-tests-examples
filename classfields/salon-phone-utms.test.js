const salonPhonesUtm = require('./salon-phone-utms');
const _ = require('lodash');

it('должен установить куку salon_phone_utms для выдачи новых подменников', () => {
    const cookie = jest.fn();
    salonPhonesUtm(
        {
            router: {
                params: {
                    utm_medium: 20,
                },
            },
            headers: {
                referer: 'http://yandex.ru',
            },
        },
        { cookie },
        _.noop,
    );
    expect(cookie).toHaveBeenCalledWith(
        'salon_phone_utms',
        'utm_medium=20&utm_source=&utm_campaign=&utm_content=',
        {
            maxAge: 604800000,
            httpOnly: true,
        },
    );
});

it('не должен установить куку, если страница неизвестна', () => {
    const cookie = jest.fn();
    salonPhonesUtm(
        {
            router: null,
            headers: {
                referer: 'http://yandex.ru',
            },
        },
        { cookie },
        _.noop,
    );
    expect(cookie).not.toHaveBeenCalled();
});

it('должен удалить куку salon_phone_utms, если среди utm-меток нет ни одной из списка допустимых', () => {
    const clearCookie = jest.fn();
    salonPhonesUtm(
        {
            router: {
                params: {
                    utm_other: 20,
                },
            },
            headers: {
                referer: 'http://yandex.ru',
            },
        },
        { clearCookie },
        _.noop,
    );
    expect(clearCookie).toHaveBeenCalled();
});

it('не должен устанавливать куку, если referer - авто.ру', () => {
    const cookie = jest.fn();
    salonPhonesUtm(
        {
            url: 'http://auto.ru/?utm_medium=20',
            headers: {
                referer: 'http://auto.ru',
            },
        },
        { cookie },
        _.noop,
    );
    expect(cookie).not.toHaveBeenCalled();
});

it('не должен устанавливать куку, если нет referer', () => {
    const cookie = jest.fn();
    salonPhonesUtm(
        {
            url: 'http://auto.ru/?utm_medium=20',
            headers: {},
        },
        { cookie },
        _.noop,
    );
    expect(cookie).not.toHaveBeenCalled();
});
