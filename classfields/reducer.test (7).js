const reducer = require('./reducer');
const {
    FAVORITES_DELETE_ITEM_RESOLVED,
    FAVORITES_DELETE_NOT_ACTIVE_ITEMS_RESOLVED,
    FAVORITES_GET_MORE_OFFERS_RESOLVED,
    FAVORITES_GET_OFFERS_RESOLVED,
} = require('./types');

const offer = {
    id: 'foo',
    hash: 'bar',
    category: 'cars',
    status: 'ACTIVE',
};

const offer2 = {
    id: 'foo2',
    hash: 'bar2',
    category: 'cars',
    status: 'ACTIVE',
};

const notActiveOffer = {
    id: 'fooSold',
    hash: 'barSold',
    category: 'cars',
    status: 'REMOVED',
};

const notActiveOffer2 = {
    id: 'fooSold2',
    hash: 'barSold2',
    category: 'cars',
    status: 'REMOVED',
};

const notActiveMotoOffer = {
    id: 'fooSoldMoto',
    hash: 'barSoldMoto',
    category: 'moto',
    status: 'REMOVED',
};

const notActiveTruckOffer = {
    id: 'fooSoldTruck',
    hash: 'barSoldTruck',
    category: 'lcv',
    status: 'REMOVED',
};

it('должен обновить стэйт при загрузке избранного', () => {
    const state = {
        data: {
            offers: [],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
        },
    };
    const action = {
        type: FAVORITES_GET_OFFERS_RESOLVED,
        payload: { offers: [ offer ] },
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer ],
            // загрузка счетчика происходит отдельно при инициализации страницы
            count: state.data.count,
        },
        pending: false,
    });
});

it('должен обновить избранное на удаление оффера из избранного', () => {
    const state = {
        data: {
            offers: [ offer, offer2 ],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 2,
        },
    };

    const action = {
        type: FAVORITES_DELETE_ITEM_RESOLVED,
        payload: { offer },
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer2 ],
            count: {
                all: 9,
                cars: 3,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 2,
        },
    });
});

it('должен обновить избранное на удаление неактивного оффера из избранного', () => {
    const state = {
        data: {
            offers: [ offer, notActiveOffer ],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 2,
        },
    };

    const action = {
        type: FAVORITES_DELETE_ITEM_RESOLVED,
        payload: { offer: notActiveOffer },
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer ],
            count: {
                all: 9,
                cars: 3,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 1,
        },
    });
});

it('должен обновить избранное на удаление всех неактивных офферов из избранного', () => {
    const state = {
        data: {
            offers: [ offer, offer2, notActiveOffer, notActiveOffer2 ],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 2,
        },
    };

    const action = {
        type: FAVORITES_DELETE_NOT_ACTIVE_ITEMS_RESOLVED,
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer, offer2 ],
            count: {
                all: 8,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 0,
        },
    });
});

it('должен обновить избранное на удаление всех неактивных офферов из избранного (вместе с мото и комтс)', () => {
    const state = {
        data: {
            offers: [ offer, offer2, notActiveOffer, notActiveOffer2, notActiveMotoOffer, notActiveTruckOffer ],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 4,
        },
    };

    const action = {
        type: FAVORITES_DELETE_NOT_ACTIVE_ITEMS_RESOLVED,
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer, offer2 ],
            count: {
                all: 6,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
            not_active_offers_count: 0,
        },
    });
});

it('должен добавлять новые офферы в избранное при подгрузке избранного', () => {
    const state = {
        data: {
            offers: [ offer ],
            count: {
                all: 10,
                cars: 4,
                moto: 3,
                trucks: 3,
            },
        },
    };

    const action = {
        type: FAVORITES_GET_MORE_OFFERS_RESOLVED,
        payload: { offers: [ offer2 ] },
    };

    expect(reducer(state, action)).toEqual({
        data: {
            offers: [ offer, offer2 ],
            count: state.data.count,
        },
        pending: false,
    });
});
