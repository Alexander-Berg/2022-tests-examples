const getUserOffersParams = require('./getUserOffersParams');

it('должен вернуть параметры для счетчика \'Размещается только на Авто.ру\'', () => {
    expect(getUserOffersParams({ params: {
        multiposting_status: 'active',
        status: 'expired',
        category: 'cars',
        section: 'new',
    }, context: { isMultipostingEnabled: true } })).toEqual({
        category: 'cars',
        section: 'new',
        status: 'expired',
        multiposting_status: 'active',
        sort: 'cr_date-desc',
        page_size: 20,
        page: undefined,
        create_date_from: undefined,
        create_date_to: undefined,
        service: undefined,
        ban_reason: undefined,
    });
});

it('должен вернуть параметры для счетчика активных объявлений для мультипостинга', () => {
    expect(getUserOffersParams({ params: {
        multiposting_status: 'active',
    }, context: { isMultipostingEnabled: true } })).toEqual({
        category: 'all',
        multiposting_status: 'active',
        sort: 'cr_date-desc',
        page_size: 20,
        page: undefined,
        create_date_from: undefined,
        create_date_to: undefined,
        service: undefined,
        ban_reason: undefined,
    });
});

it('должен вернуть параметры для счетчика забаненных объявлений', () => {
    expect(getUserOffersParams({ params: {
        status: 'banned',
    }, context: { isMultipostingEnabled: true } })).toEqual({
        category: 'all',
        status: 'banned',
        sort: 'cr_date-desc',
        page_size: 20,
        page: undefined,
        create_date_from: undefined,
        create_date_to: undefined,
        service: undefined,
        ban_reason: undefined,
    });
});
