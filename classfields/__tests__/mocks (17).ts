export const defaultStore = {
    user: {},
    geo: { rgid: 1 },
    config: {
        view: 'desktop',
    },
};

export const storeWithDefaultEmail = {
    ...defaultStore,
    user: { defaultEmail: 'email@yandex.ru' },
    searchHistory: {
        currentSearchData: {
            searchHistoryParams: {
                serialized: {
                    title: 'Объявления: купить квартиру, 1-комнатные, 2-комнатные, цена от 3 млн руб.',
                    body: 'в Москве и МО, вторичка, метро: Улица 1905 года',
                },
            },
        },
    },
};
