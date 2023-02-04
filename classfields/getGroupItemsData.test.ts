import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerCarsMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { CabinetOffer } from 'www-cabinet/react/components/SaleButtonsRoyal/types';

import type { ItemsConfig } from './getItemsData';
import { getGroupItemsData } from './getGroupItemsData';

const offer = cloneOfferWithHelpers(offerCarsMock)
    .withPrice(385000)
    .withMultiposting()
    .withCabinetServices()
    .value() as CabinetOffer;

const offerWithAuc = cloneOfferWithHelpers(offerCarsMock)
    .withPrice(385000)
    .withMultiposting()
    .withCabinetServices()
    .withAuction()
    .value() as CabinetOffer;

const offerWithFav = cloneOfferWithHelpers(offerCarsMock)
    .withPrice(385000)
    .withMultiposting()
    .withCabinetServices()
    .withAuction()
    .withFavoriteCounters()
    .value() as CabinetOffer;

const dataMock = {
    isDisabled: false,
    isMultiposting: false,
    isNew: false,
    length: 0,
    favoriteCounters: null,
    size: 'm',
    offers: [ { ...offer, id: '1114948550' } ],
    checkedSalesIds: { '1114948550': true },
    context: {
        metrika: {},
        pageParams: {
            category: 'cars',
            section: 'used',
            auction: 'true',
            resetSales: 'true',
        },
    },
    buttonsPriority: {
        other: [
            'fresh',
            'turbo',
            'favorite',
            'premium',
            'spec',
            'sticker',
        ],
        carsnew: [
            'fresh',
            'premium',
            'favorite',
            'sticker',
        ],
    },
    applySaleServiceForList: jest.fn(),
};

const dataMockAuc = {
    ...dataMock,
    offers: [ { ...offerWithAuc, id: '1114948550',
        service_fresh: {
            active: false,
            isFetching: false,
            date: '',
            tooltip: {},
        }, service_premium: {
            active: false,
            isFetching: false,
            date: '',
            tooltip: {},
        }, service_turbo: {
            active: false,
            isFetching: false,
            date: '',
            tooltip: {},
        } } ],
};

const dataMockFav = {
    ...dataMock,
    offers: [ { ...offerWithFav, id: '1114948550' }, { ...offerWithFav, id: '3331133' } ],
};

it('getItemsData вернет набор васов для бу', () => {
    expect(getGroupItemsData(dataMock as any as ItemsConfig)
        ?.map(item => item && item?.alias)
        .filter(Boolean))
        .toMatchSnapshot();
});

it('getItemsData вернет набор васов для бу аукцион', () => {
    expect(getGroupItemsData(dataMockAuc as any as ItemsConfig)
        ?.map(item => item && item?.alias)
        .filter(Boolean))
        .toMatchSnapshot();
});

it('getItemsData вернет favorite', () => {
    expect(getGroupItemsData(dataMockFav as any as ItemsConfig)
        ?.map(item => item && item?.alias)
        .filter(Boolean))
        .toMatchSnapshot();
});
