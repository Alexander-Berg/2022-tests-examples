import noop from 'lodash/noop';

export const date = new Date(2021, 8, 28);

export interface IGate {
    create(): Promise<void>;
}

export const GatePending: IGate = {
    create: () => new Promise(noop),
};

export const GateSuccess: IGate = {
    create: () => Promise.resolve(),
};

export const GateError: IGate = {
    create: () => Promise.reject(),
};

export const conciergeExtraValues = {
    date: new Date('2021-09-29T00:00:00').valueOf(),
    time: '9-13',
};

export const userMock = {
    crc: 'u6aufj490g4c0c3fb9e34612340215c1',
    uid: '1234123412',
    yuid: '2089203918273403541',
    isVosUser: false,
    isAuth: true,
    isJuridical: false,
    paymentTypeSuffix: 'natural',
    promoSubscription: {},
    avatarId: '0/0-0',
    avatarHost: 'avatars.mdst.yandex.net',
    defaultEmail: 'user.test@yandex.ru',
    emailHash: 'dkl1231kdsdc2b6c3112c80ed404c',
    defaultPhone: '+79999999999',
    passHost: 'https://pass-test.yandex.ru',
    passportHos: 'https://passport-test.yandex.ru',
    passportApiHost: 'https://api.passport-test.yandex.ru',
    passportOrigin: 'realty_saint-petersburg',
    passportDefaultEmail: 'user.test@yandex.ru',
    favorites: [],
    favoritesMap: {},
    comparison: [],
    statistics: {},
    displayName: 'Тестовый юзер',
};
