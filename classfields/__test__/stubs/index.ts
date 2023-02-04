import { DeepPartial } from 'utility-types';

import { IConfigStore } from 'realty-core/view/react/common/reducers/config';
import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';
import { IYandexArendaStore } from 'realty-core/view/react/modules/yandex-arenda/redux/reducer';

import { IPageStore } from 'realty-core/view/react/common/reducers/page';

import { IPassport } from 'types/passport';

import { IUserStore } from 'view/modules/user/reducers';
import { IPassportAccountsStore } from 'view/modules/passportAccounts/reducers';
import { IServiceLinksStore } from 'view/modules/serviceLinks/reducers';

type IRequiredStore = {
    passport: DeepPartial<IPassport>;
    passportAccounts: DeepPartial<IPassportAccountsStore>;
    config: DeepPartial<IConfigStore>;
    user: DeepPartial<IUserStore>;
    geo: DeepPartial<IGeoStore>;
    yandexArenda: DeepPartial<IYandexArendaStore>;
    page: DeepPartial<IPageStore>;
    serviceLinks: DeepPartial<IServiceLinksStore>;
};

export const mobileStore: IRequiredStore = {
    passport: {
        defaultPhone: '',
        defaultEmail: '',
        isAuth: false,
        phones: [],
        emails: [],
    },
    passportAccounts: {
        accounts: [],
    },
    config: {
        isMobile: 'iPad',
        retpath: 'https://arenda.test.vertis.yandex.ru/',
        rootUrl: 'https://realty.test.vertis.yandex.ru',
        realtyUrl: 'ttps://realty.test.vertis.yandex.ru/',
    },
    user: {
        calculatedInfo: {},
    },
    geo: {
        id: 2,
        type: 'SUBJECT_FEDERATION',
        rgid: 741965,
        name: 'Санкт-Петербург и ЛО',
        locative: 'в Санкт-Петербурге и ЛО',
    },
    yandexArenda: {
        isLoading: false,
        isSuccess: false,
        savedPhone: '',
    },
    page: {
        route: 'landing-owner',
    },
    serviceLinks: {
        ARENDA_SEARCH_URL: 'https://realty.ru',
    },
};
