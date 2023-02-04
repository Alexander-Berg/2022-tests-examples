jest.mock('auto-core/react/lib/offer/hasStateSupport');

const React = require('react');
const { Provider } = require('react-redux');

const { shallow } = require('enzyme');
import { render } from '@testing-library/react';
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cardMotorcycleMock = require('auto-core/react/dataDomain/card/mocks/card.motorcycle.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const CardOfferBody = require('./CardOfferBody').default;

describe('блок Получить лучшую цену', () => {
    let offer;
    beforeEach(() => {
        offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .value();
    });

    it('должен нарисовать, если оффер новый и есть нужный контекст', () => {
        const bestPriceOffer = cloneOfferWithHelpers(offer).withSection('new').withMatchApplicationContexts([ 'footer' ]).value();

        const wrapper = shallowRenderWrapper({ offer: bestPriceOffer, tutorialLinks: [], isCardNew: true });

        expect(wrapper.find('CardBestPriceDesktop')).toExist();
    });

    it('не должен рисовать, если контекст нужный, но оффер б/у', () => {
        const bestPriceOffer = cloneOfferWithHelpers(offer).withMatchApplicationContexts([ 'footer' ]).value();

        const wrapper = shallowRenderWrapper({ offer: bestPriceOffer, tutorialLinks: [], isCardNew: true });

        expect(wrapper.find('CardBestPriceDesktop')).not.toExist();
    });

    it('не должен рисовать блок, если оффер новый, но нет нужного контекста', () => {
        const bestPriceOffer = cloneOfferWithHelpers(offer).withSection('new').value();

        const wrapper = shallowRenderWrapper({ offer: bestPriceOffer, tutorialLinks: [], isCardNew: true });

        expect(wrapper.find('CardBestPriceDesktop')).not.toExist();
    });
});

it('не должен нарисовать блок с кнопкой бронирования, если дилер в blacklist', () => {
    const offer = cloneOfferWithHelpers(cardMock)
        .withSellerTypeCommercial()
        .withSection('used')
        .withIsOwner(false)
        .withCategory('cars')
        .withSalon()
        .value();

    const wrapper = shallowRenderWrapper({
        offer,
        tutorialLinks: [],
        bookingPopup: {
            isBookingFeatureEnabled: true,
            withoutBookingSalons: [ 7806 ],
        },
    });

    expect(wrapper.find('Connect(BookingDumb)').exists()).toBe(false);
});

describe('программа господдержки', () => {
    it('покажет скидку, если оффер входит в программу', () => {
        const offer = cloneOfferWithHelpers(cardMock).withSection('new').value();

        const wrapper = shallowRenderWrapper({
            offer,
            tutorialLinks: [],
            hasStateSupport: true,
            isCardNew: true,
        });

        const discountList = wrapper.find('CardDiscountList');

        expect(discountList.prop('hasStateSupport')).toBe(true);
    });

    it('не покажет скидку, если оффер не входит в программу', () => {
        const offer = cloneOfferWithHelpers(cardMock).withSection('new').value();

        const wrapper = shallowRenderWrapper({
            offer,
            tutorialLinks: [],
            isCardNew: true,
        });

        const discountList = wrapper.find('CardDiscountList');

        expect(discountList.isEmptyRender()).toBe(true);
    });
});

describe('блок с комплектацией', () => {
    describe('новые легковые', () => {
        let offer;
        beforeEach(() => {
            offer = cloneOfferWithHelpers(cardMock)
                .withCategory('cars')
                .withSection('new')
                .value();
        });

        it('должен нарисовать блок с комплектацией', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
                isCardNew: true,
            });

            const cardComplectation = wrapper.find('Connect(CardComplectation)');
            expect(cardComplectation).toHaveProp('offer', offer);
        });

        it('должен нарисовать блок с комплектацией перед трейд-ин и описанием, после галереи', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
                isCardNew: true,
            }).children();

            expect(wrapper.at(6).is('.CardOfferBody__columnsWrapper')).toEqual(true);
            expect(wrapper.at(6).find('Connect(CardImageGallery)')).toExist(true);
            expect(wrapper.at(7).is('Connect(CardComplectation)')).toEqual(true);
            expect(wrapper.at(9).is('CardDescription')).toEqual(true);
            expect(wrapper.at(12).is('Connect(CardTradeInDesktop)')).toEqual(true);
        });
    });

    describe('бу легковые', () => {
        let offer;
        beforeEach(() => {
            offer = cloneOfferWithHelpers(cardMock)
                .withCategory('cars')
                .withSection('used')
                .value();
        });

        it('должен нарисовать блок с комплектацией', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            });

            const cardComplectation = wrapper.find('Connect(CardComplectation)');
            expect(cardComplectation).toHaveProp('offer', offer);
        });

        it('должен нарисовать блок с комплектацией после галереи, описания, пресетов чата и перед трейд-ин', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            }).children();

            expect(wrapper.at(6).is('.CardOfferBody__columnsWrapper')).toEqual(true);
            expect(wrapper.at(6).find('Connect(CardImageGallery)')).toExist(true);
            expect(wrapper.at(8).is('CardDescription')).toEqual(true);
            expect(wrapper.at(9).is('Connect(CardChatPresets)')).toEqual(true);
            expect(wrapper.at(10).is('Connect(CardComplectation)')).toEqual(true);
            expect(wrapper.at(15).is('Connect(CardTradeInDesktop)')).toEqual(true);
        });
    });

    describe('бу мотоциклы', () => {
        let offer;
        beforeEach(() => {
            offer = cloneOfferWithHelpers(cardMotorcycleMock)
                .withCategory('moto')
                .withSubCategory('motorcycle')
                .withSection('used')
                .value();
        });

        it('должен нарисовать блок с комплектацией', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            });

            const cardComplectation = wrapper.find('Connect(CardComplectation)');
            expect(cardComplectation).toHaveProp('offer', offer);
        });

        it('должен нарисовать блок с комплектацией после галереи, описания, пресетов чата и перед трейд-ин', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            }).children();

            expect(wrapper.at(7).is('.CardOfferBody__columnsWrapper')).toEqual(true);
            expect(wrapper.at(7).find('Connect(CardImageGallery)')).toExist(true);
            expect(wrapper.at(8).is('CardDescription')).toEqual(true);
            expect(wrapper.at(9).is('Connect(CardChatPresets)')).toEqual(true);
            expect(wrapper.at(10).is('Connect(CardComplectation)')).toEqual(true);
            expect(wrapper.at(15).is('Connect(CardTradeInDesktop)')).toEqual(true);
        });
    });

    describe('новые мотоциклы', () => {
        let offer;
        beforeEach(() => {
            offer = cloneOfferWithHelpers(cardMotorcycleMock)
                .withCategory('moto')
                .withSubCategory('motorcycle')
                .withSection('new')
                .value();
        });

        it('должен нарисовать блок с комплектацией', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            });

            const cardComplectation = wrapper.find('Connect(CardComplectation)');
            expect(cardComplectation).toHaveProp('offer', offer);
        });

        it('должен нарисовать блок с комплектацией после галереи, описания, пресетов чата и перед трейд-ин', () => {
            const wrapper = shallowRenderWrapper({
                offer,
                tutorialLinks: [],
            }).children();

            expect(wrapper.at(7).is('.CardOfferBody__columnsWrapper')).toEqual(true);
            expect(wrapper.at(7).find('Connect(CardImageGallery)')).toExist(true);
            expect(wrapper.at(8).is('CardDescription')).toEqual(true);
            expect(wrapper.at(9).is('Connect(CardChatPresets)')).toEqual(true);
            expect(wrapper.at(10).is('Connect(CardComplectation)')).toEqual(true);
            expect(wrapper.at(15).is('Connect(CardTradeInDesktop)')).toEqual(true);
        });
    });
});

describe('баннер о грузовом такси для б/у оффера лёгкого комтранса', () => {
    const props = { isCardNew: false, tutorialLinks: [] };

    it('должен отрисовать баннер', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withSection('used')
            .withCategory('trucks')
            .withSubCategory('lcv')
            .value();

        const wrapper = shallowRenderWrapper({ ...props, offer });

        expect(wrapper.find('Connect(TaxiPromoForTruckDrivers)').exists()).toBe(true);
    });

    it('должен отрисовать баннер для нового лёгкого комтранса', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withSection('new')
            .withCategory('trucks')
            .withSubCategory('lcv')
            .value();

        const wrapper = shallowRenderWrapper({ ...props, offer });

        expect(wrapper.find('Connect(TaxiPromoForTruckDrivers)').exists()).toBe(false);
    });

    it('не должен отрисовать баннер для легковых', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('cars')
            .value();

        const wrapper = shallowRenderWrapper({ ...props, offer });

        expect(wrapper.find('Connect(TaxiPromoForTruckDrivers)').exists()).toBe(false);
    });

    it('не должен отрисовать баннер для мото', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCategory('moto')
            .value();

        const wrapper = shallowRenderWrapper({ ...props, offer });

        expect(wrapper.find('Connect(TaxiPromoForTruckDrivers)').exists()).toBe(false);
    });
});

describe('OwnerVasBlock эксп AUTORUFRONT-19219_new_lk_and_vas_block_design', () => {
    const defaultOffer = cloneOfferWithHelpers(cardMock)
        .withIsOwner(true)
        .value();

    const hasExperiment = exp => exp === 'AUTORUFRONT-19219_new_lk_and_vas_block_design';
    const props = { isCardNew: false, tutorialLinks: [], offer: defaultOffer };

    it('рендерит блок, если есть эксп, оффер владельца, активный и не салон', () => {
        const wrapper = shallowRenderWrapper(props, hasExperiment);

        expect(wrapper.find('OwnerVasBlock')).toExist();
    });

    it('НЕ рендерит блок, если есть эксп, оффер владельца, активный и оффер НЕ частника / перекупа', () => {
        const offer = cloneOfferWithHelpers(defaultOffer).withSellerTypeCommercial().withSalon().value();
        const wrapper = shallowRenderWrapper({ ...props, offer }, hasExperiment);

        expect(wrapper.find('OwnerVasBlock')).not.toExist();
    });

    it('НЕ рендерит блок, если есть эксп, оффер владельца, не салон и оффер НЕ активный', () => {
        const offer = cloneOfferWithHelpers(defaultOffer).withStatus('BANNED').value();
        const wrapper = shallowRenderWrapper({ ...props, offer }, hasExperiment);

        expect(wrapper.find('OwnerVasBlock')).not.toExist();
    });

    it('НЕ рендерит блок, если есть эксп, активный, не салон и оффер НЕ владельца', () => {
        const offer = cloneOfferWithHelpers(defaultOffer).withIsOwner(false).value();
        const wrapper = shallowRenderWrapper({ ...props, offer }, hasExperiment);

        expect(wrapper.find('OwnerVasBlock')).not.toExist();
    });

    it('НЕ рендерит блок, если оффер владельца, активный и не салон и НЕТ экспа', () => {
        const wrapper = shallowRenderWrapper(props);

        expect(wrapper.find('OwnerVasBlock')).not.toExist();
    });
});

describe('метрика ссылки на публичную страницу перекупа', () => {
    const Context = createContextProvider(contextMock);
    const initialState = {
        autoPopup: {},
        ads: { data: {} },
        bunker: {},
        tradein: { tradeinPrice: {} },
        credit: { banks: { data: {} }, products: { data: {} } },
        user: { data: {} },
        cardGroupComplectations: { data: {} },
        card: cardMock,
        publicUserInfo: { data: {} },
    };

    it('отправит метрику на показ, если у пользователя открытый профиль', () => {
        const offer = cloneOfferWithHelpers(cardMock).withEncryptedUserId('some_encrypted_id').value();
        const props = { isCardNew: false, tutorialLinks: [], offer };
        render(
            <Provider store={ mockStore({ ...initialState, card: offer }) }>
                <Context>
                    <CardOfferBody { ...props }/>
                </Context>
            </Provider>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
        expect(contextMock.metrika.sendPageEvent.mock.calls[2][0]).toEqual([ 'reseller_public', 'link-show' ]);
    });

    it('не отправит метрику на показ, если у пользователя нет открытого профиля', () => {
        const props = { isCardNew: false, tutorialLinks: [], offer: cardMock };

        render(
            <Provider store={ mockStore(initialState) }>
                <Context>
                    <CardOfferBody { ...props }/>
                </Context>
            </Provider>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
    });
});

const shallowRenderWrapper = (props, hasExperiment) => {
    const Context = createContextProvider({
        ...contextMock,
        ...hasExperiment && { hasExperiment },
    });

    props.isCardNew = props.isCardNew || false;

    return shallow(
        <Context>
            <CardOfferBody { ...props }/>
        </Context>,
    ).dive();
};
