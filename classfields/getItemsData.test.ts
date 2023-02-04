import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerCarsMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { CabinetOffer } from 'www-cabinet/react/components/SaleButtonsRoyal/types';

import type { ItemsConfig } from './getItemsData';
import { getItemsData } from './getItemsData';

const offer = cloneOfferWithHelpers(offerCarsMock)
    .withPrice(385000)
    .withMultiposting()
    .withCabinetServices()
    .value() as CabinetOffer;

const offerWithAuc = cloneOfferWithHelpers(offerCarsMock)
    .withPrice(385000)
    .withMultiposting()
    .withCabinetServices({
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
        },
    })
    .withAuction()
    .value() as CabinetOffer;

const dataMock = {
    isDisabled: false,
    isMultiposting: false,
    isNew: false,
    length: 0,
    favoriteCounters: null,
    size: 'm',
    offer: offer,
    offerID: '1114948550-d1434275',
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
    applyService: jest.fn(),
    applySaleService: jest.fn(),
    showBadgesSettings: jest.fn(),
    showSaleFreshConfirmModal: jest.fn(),
};

const dataMockAuc = {
    ...dataMock,
    offer: offerWithAuc,
};

it('getItemsData вернет набор васов для бу', () => {
    expect(getItemsData(dataMock as any as ItemsConfig)
        ?.map(item => item && item?.alias)
        .filter(Boolean))
        .toMatchSnapshot();
});

it('getItemsData вернет набор васов для бу аукцион', () => {
    expect(getItemsData(dataMockAuc as any as ItemsConfig)
        ?.map(item => item && item?.alias)
        .filter(Boolean))
        .toMatchSnapshot();
});
