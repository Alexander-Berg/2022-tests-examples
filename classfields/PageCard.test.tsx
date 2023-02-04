/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { AdditionalInfo, State } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import listingMock from 'autoru-frontend/mockData/state/listing';

import getNumberOfViewedOffersForCurrentSession from 'auto-core/lib/viewedOffers/getNumberOfViewedOffersForCurrentSession';

import defaultVinReportMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/defaultVinReport.mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import PageCard from './PageCard';

jest.mock('auto-core/lib/viewedOffers/getNumberOfViewedOffersForCurrentSession');

let store: MobileAppState;
let BASE_CARD: Partial<Offer>;
let BASE_STORE: Record<string, any>;
beforeEach(() => {
    BASE_STORE = {
        bunker: {},
        config: configStateMock.value(),
        user: { data: {} },
        publicUserInfo: { data: {} },
    };

    BASE_CARD = {
        additional_info: { is_owner: false } as AdditionalInfo,
        category: 'cars',
        id: '123',
        hash: 'abc',
        section: 'used',
        state: {} as State,
        status: OfferStatus.ACTIVE,
    };
});

describe('Компонент "Позвонить"', () => {
    let offer: TOfferMock;
    beforeEach(() => {
        offer = cloneOfferWithHelpers(BASE_CARD)
            .withStatus(OfferStatus.ACTIVE);
    });

    it('не должен показывать блок "позвонить" для владельца', () => {
        const wrapper = renderPageCard(offer.withIsOwner(true).value());

        expect(wrapper.find('Connect(OfferFloatPhones)')).toHaveLength(0);
    });

    it('должен показывать блок "позвонить" для не владельца', () => {
        const wrapper = renderPageCard(offer.withIsOwner(false).value());

        expect(wrapper.find('Connect(OfferFloatPhones)')).toHaveLength(1);
    });

});

describe('Проданное объявление', () => {
    let wrapper: ShallowWrapper;
    beforeEach(() => {
        const card = cloneOfferWithHelpers(BASE_CARD).withStatus(OfferStatus.INACTIVE);
        wrapper = renderPageCard(card.value());
    });

    it('отрисовать компонент про проданную машину (CardSold)', () => {
        expect(wrapper.find('CardSold')).toHaveLength(1);
    });

    it('не должен отрисовать компонент с контактами продавца (OfferSellerInfo)', () => {
        expect(wrapper.find('OfferSellerInfo')).toHaveLength(0);
    });

    it('не должен отрисовать компонент жалобы (CardBottomLinks)', () => {
        expect(wrapper.find('CardBottomLinks')).toHaveLength(0);
    });

    it('не должен отрисовать компонент с действиями (CardActions)', () => {
        expect(wrapper.find('CardActions')).toHaveLength(0);
    });

    it('не должен показывать блок с отчетом VinHistory', () => {
        expect(wrapper.find('Connect(VinHistoryDumb)')).toHaveLength(0);
    });

    it('не должен показывать блок "позвонить"', () => {
        expect(wrapper.find('Connect(OfferFloatPhones)')).toHaveLength(0);
    });
});

describe(`'блок VAS'ов'`, () => {
    let offer: TOfferMock;
    const VAS_BLOCK_SELECTOR = 'Connect(VasBlock)';
    beforeEach(() => {
        offer = cloneOfferWithHelpers(BASE_CARD)
            .withIsOwner(true)
            .withSellerTypePrivate()
            .withStatus(OfferStatus.ACTIVE);
    });

    it('должен нарисовать для владельца частника', () => {
        const wrapper = renderPageCard(offer.value());
        expect(wrapper.find(VAS_BLOCK_SELECTOR)).toHaveLength(1);
    });

    it('не должен нарисовать для владельца дилера', () => {
        offer = offer.withSellerTypeCommercial();
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find(VAS_BLOCK_SELECTOR)).toHaveLength(0);
    });

    it('не должен нарисовать для невладельца', () => {
        offer = offer.withIsOwner(false);
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find(VAS_BLOCK_SELECTOR)).toHaveLength(0);
    });

    it('не должен нарисовать для неактивного объявления', () => {
        offer = offer.withStatus(OfferStatus.INACTIVE);
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find(VAS_BLOCK_SELECTOR)).toHaveLength(0);
    });
});

describe('Компонент CardSellerAlarms', () => {
    let offer: TOfferMock;
    beforeEach(() => {
        offer = cloneOfferWithHelpers(BASE_CARD)
            .withIsOwner(false)
            .withStatus(OfferStatus.ACTIVE);
    });

    it('должен отреднерить для активного объявления и не владельнца', () => {
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('CardSellerAlarms')).toExist();
    });

    it('не должен отреднерить для неактивного объявления', () => {
        const wrapper = renderPageCard(offer.withStatus(OfferStatus.INACTIVE).value());

        expect(wrapper.find('CardSellerAlarms')).not.toExist();
    });

    it('не должен отреднерить для владельца', () => {
        const wrapper = renderPageCard(offer.withIsOwner(true).value());

        expect(wrapper.find('CardSellerAlarms')).not.toExist();
    });
});

describe('метрики показа', () => {
    let offer: TOfferMock;
    beforeEach(() => {
        offer = cloneOfferWithHelpers(BASE_CARD)
            .withIsOwner(false)
            .withStatus(OfferStatus.ACTIVE);
    });

    it('должен отправить цель на открытие карточки мото', () => {
        BASE_STORE.config.data = { pageParams: { category: 'moto' } };
        renderPageCard(offer.value());

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('MAUTORU_SALE_MOTO_SHOW');
    });

    it('должен отправить цель на открытие карточки комтранса', () => {
        BASE_STORE.config.data = { pageParams: { category: 'trucks' } };
        renderPageCard(offer.value());

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('MAUTORU_SALE_TRUCKS_SHOW');
    });

    it('не должен отправить цель на открытие карточки легковых', () => {
        BASE_STORE.config.data = { pageParams: { category: 'cars' } };
        renderPageCard(offer.value());

        expect(contextMock.metrika.reachGoal).not.toHaveBeenCalled();
    });

    it('отправит цель о просмотре 5 офферов за сессию', () => {
        (getNumberOfViewedOffersForCurrentSession as jest.MockedFunction<typeof getNumberOfViewedOffersForCurrentSession>).mockReturnValueOnce(5);
        renderPageCard(offer.value());
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('SHOW_FIVE_CARD');

    });

    it('не отправит цель о просмотре 4 офферов за сессию', () => {
        (getNumberOfViewedOffersForCurrentSession as jest.MockedFunction<typeof getNumberOfViewedOffersForCurrentSession>).mockReturnValueOnce(4);
        renderPageCard(offer.value());
        expect(contextMock.metrika.reachGoal).not.toHaveBeenCalledWith('SHOW_FIVE_CARD');

    });
});

describe('Провязка "Спецпредложения"', () => {
    let offer: TOfferMock;
    beforeEach(() => {
        offer = cloneOfferWithHelpers(BASE_CARD)
            .withIsOwner(false)
            .withStatus(OfferStatus.ACTIVE);
    });

    it('должен отреднерить блок', () => {
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('WidgetSpecialOffers')).toExist();
    });

    it('должен отреднерить блок и передать параметры для запроса', () => {
        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('WidgetSpecialOffers')).toHaveProp('resourceName', 'cardCarsSpecial');
        expect(wrapper.find('WidgetSpecialOffers')).toHaveProp('resourceParams', { offerID: '123-abc', category: 'cars' });
        expect(wrapper.find('WidgetSpecialOffers')).toHaveProp('searchID', '');
    });

    it('должен отреднерить блок и передать search_id из листинга', () => {
        BASE_STORE.listing = listingMock;
        BASE_STORE.searchID = {
            searchID: listingMock.searchID,
            parentSearchId: undefined,
        };

        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('WidgetSpecialOffers')).toHaveProp('searchID', 'd8209a38121e9696dc3015e273c31195b6d7933d');
    });
});

describe('Блок показать лучшую цену в галерее', () => {
    it('показать блок когда если оффер new, контекст gallery', () => {
        const offer = cloneOfferWithHelpers(offerMock).withSection('new').withMatchApplicationContexts([ 'gallery' ]);

        const wrapper = renderPageCard(offer.value());

        const cardGallery = wrapper.find('Connect(CardGallery)');

        expect(cardGallery).toHaveProp('shouldShowBestPriceBlock', true);
    });

    it('не показывать блок когда если оффер used, контекст gallery', () => {
        const offer = cloneOfferWithHelpers(offerMock).withMatchApplicationContexts([ 'gallery' ]);

        const wrapper = renderPageCard(offer.value());

        const cardGallery = wrapper.find('Connect(CardGallery)');

        expect(cardGallery).toHaveProp('shouldShowBestPriceBlock', false);
    });

    it('не показывать блок когда если оффер new, если нет контекста gallery', () => {
        const offer = cloneOfferWithHelpers(offerMock).withSection('new');

        const wrapper = renderPageCard(offer.value());

        const cardGallery = wrapper.find('Connect(CardGallery)');

        expect(cardGallery).toHaveProp('shouldShowBestPriceBlock', false);
    });
});

describe('Блок показать лучшую цену в футере', () => {
    it('показать блок если оффер new, контекст footer', () => {
        const offer = cloneOfferWithHelpers(offerMock).withSection('new').withMatchApplicationContexts([ 'footer' ]);

        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('CardBestPriceMobile')).toExist();
    });

    it('не показывать блок если оффер used и контекст footer', () => {
        const offer = cloneOfferWithHelpers(offerMock).withMatchApplicationContexts([ 'footer' ]);

        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('CardBestPriceMobile')).not.toExist();
    });

    it('не показывать блок если оффер new и нет контекста footer', () => {
        const offer = cloneOfferWithHelpers(offerMock).withSection('new');

        const wrapper = renderPageCard(offer.value());

        expect(wrapper.find('CardBestPriceMobile')).not.toExist();
    });
});

describe('Компонент "Отчёт о проверке по VIN"', () => {
    const offerWithReport = cloneOfferWithHelpers(BASE_CARD)
        .withStatus(OfferStatus.ACTIVE)
        .withSection('used')
        .withCategory('cars')
        .value();

    const renderPageCard = (customStore?: MobileAppState) => {
        const store = mockStore(customStore || BASE_STORE);

        return shallow(
            <PageCard params={{}}/>,
            { context: { ...contextMock, store } },
        ).dive().find('Connect(VinHistoryDumb)').dive().dive();
    };

    it('должен отреднерить блок с отчётом, если есть отчёт', () => {
        const wrapper = renderPageCard({
            ...BASE_STORE,
            card: offerWithReport,
            vinReport: defaultVinReportMock,
        } as unknown as MobileAppState);

        expect(wrapper.find('Loadable').dive().find('CardVinReport')).toExist();
    });

    it('должен отреднерить скелетон для отчёта, если отчёта ещё нет', () => {
        const wrapper = renderPageCard({
            ...BASE_STORE,
            card: offerWithReport,
            vinReport: {},
        } as unknown as MobileAppState);

        expect(wrapper.find('ForwardRef(CardVinReportTemplate)')).toExist();
    });
});

function renderPageCard(offer: Offer, hasExperiment?: (exp: string) => boolean): ShallowWrapper {
    store = mockStore({
        ...BASE_STORE,
        card: offer,
    }) as Partial<MobileAppState> as MobileAppState;

    const wrapper = shallow(
        <PageCard
            params={{}}
        />,
        { context: {
            ...contextMock,
            store,
            ...hasExperiment && { hasExperiment },
        } },
    ).dive();

    return wrapper;
}
