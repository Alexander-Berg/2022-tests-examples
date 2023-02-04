jest.mock('www-cabinet/react/dataDomain/sales/helpers/hasCallsData');
const getIsShowingCallsSorts = require('www-cabinet/react/dataDomain/sales/selectors/getIsShowingCallsSorts');

it('должен вернуть true, если category === "all" и у пользователя в доступных ему категориях hasCallsData = true', () => {
    const hasCallsData = require('www-cabinet/react/dataDomain/sales/helpers/hasCallsData');
    hasCallsData.mockImplementation(() => true);
    const state = {
        user: {
            data: {
                clientUrls: [ {
                    name: 'Легковые новые',
                    tag: 'cars_new',
                    url: 'https://frontend.melnik88.dev.vertis.yandex.net/cars/new/add/',
                } ],
            },
        },
        sales: {
            callTrackingSettings: {

            },
            searchParams: {
                category: 'all',
            },
        },
    };

    expect(getIsShowingCallsSorts(state)).toBe(true);
});

it('должен вернуть true, если category === "cars" и в этой категории hasCallsData = true', () => {
    const hasCallsData = require('www-cabinet/react/dataDomain/sales/helpers/hasCallsData');
    hasCallsData.mockImplementation(() => true);
    const state = {
        user: {
            data: {
                clientUrls: [ {
                    name: 'Легковые новые',
                    tag: 'cars_new',
                    url: 'https://frontend.melnik88.dev.vertis.yandex.net/cars/new/add/',
                } ],
            },
        },
        sales: {
            callTrackingSettings: {

            },
            searchParams: {
                category: 'cars',
            },
        },
    };
    expect(getIsShowingCallsSorts(state)).toBe(true);
});

it('должен вернуть false, если category === "cars" и в этой категории hasCallsData = false', () => {
    const hasCallsData = require('www-cabinet/react/dataDomain/sales/helpers/hasCallsData');
    hasCallsData.mockImplementation(() => false);
    const state = {
        user: {
            data: {
                clientUrls: [ {
                    name: 'Легковые новые',
                    tag: 'cars_new',
                    url: 'https://frontend.melnik88.dev.vertis.yandex.net/cars/new/add/',
                } ],
            },
        },
        sales: {
            callTrackingSettings: {

            },
            searchParams: {
                category: 'cars',
            },
        },
    };
    expect(getIsShowingCallsSorts(state)).toBe(false);
});
