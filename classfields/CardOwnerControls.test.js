/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/card/actions/toggleAutoProlongation');
jest.mock('auto-core/react/dataDomain/card/actions/offerActivate');
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/card/actions/updateOffer');

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const MockDate = require('mockdate');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const toggleAutoProlongation = require('auto-core/react/dataDomain/card/actions/toggleAutoProlongation');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const offerActivate = require('auto-core/react/dataDomain/card/actions/offerActivate');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const { SECOND } = require('auto-core/lib/consts');

const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
paymentModalOpen.mockImplementation(() => () => {});

const updateOffer = require('auto-core/react/dataDomain/card/actions/updateOffer');
updateOffer.mockImplementation(() => () => {});

const CardOwnerControls = require('./CardOwnerControls');

let store;
beforeEach(() => {
    store = mockStore({
        bunker: getBunkerMock([ 'common/activate_in_app' ]),
        config: configStateMock.value(),
        user: { data: {} },
    });
});

afterEach(() => {
    MockDate.reset();
});

it('должен отрендерить контролы для владельца, активное объявление', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('не должен отрендерить контролы для невладельца', () => {
    const offer = {
        additional_info: { is_owner: false, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    expect(shallowToJson(wrapper)).toHaveLength(0);
});

it('должен отрендерить доп инфо для перекупа с форсированным продлением размещения', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: '123' },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
        service_prices: [ { service: 'all_sale_activate', prolongation_forced_not_togglable: true } ],
        services: [ { service: 'all_sale_activate', auto_prolong_price: 999 } ],
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    expect(shallowToJson(wrapper.find('PlacementProlongationInfoDesktop'))).toMatchSnapshot();
});

it('должен отрендерить кнопку редактирования для дилера, если есть права на редактирование объявления', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
        seller_type: 'COMMERCIAL',
    };

    store = mockStore({
        bunker: getBunkerMock([ 'common/activate_in_app' ]),
        config: configStateMock.value(),
        user: {
            data: {
                access: {
                    grants: [ {
                        access: 'READ_WRITE',
                        alias: 'OFFERS',
                    } ],
                },
                profile: {
                    autoru: {
                        client_id: '16453',
                    },
                },
            },
        },
    });

    const wrapper = shallow(
        <CardOwnerControls offer={ offer } canW/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    expect(wrapper.find('Link[children="Редактировать"]')).toHaveLength(1);
});

it('не должен отрендерить кнопку редактирования для дилера, если нет прав на редактирование объявления', () => {
    store = mockStore({
        bunker: getBunkerMock([ 'common/activate_in_app' ]),
        config: configStateMock.value(),
        user: {
            data: {
                access: {
                    grants: [ {
                        access: 'READ_ONLY',
                        alias: 'OFFERS',
                    } ],
                },
                profile: {
                    autoru: {
                        client_id: '16453',
                    },
                },
            },
        },
    });

    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
        seller_type: 'COMMERCIAL',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer } canW/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    expect(wrapper.find('Link[children="Редактировать"]')).toHaveLength(0);
});

it('должен быть статус "В архиве", удаленное объявление', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'REMOVED',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();
    expect(shallowToJson(wrapper.find('div[children="В архиве"]'))).not.toBeNull();
});

it('Не должно быть кнопки "редактировать", если оффер не редактируется', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: false, hide: true },
        category: 'cars',
        status: 'REMOVED',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();
    expect(shallowToJson(wrapper.find('Link[children="Редактировать"]'))).toBeNull();
});

it('должна быть кнопка "Опубликовать", если объявление можно активировать', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: false, activate: true },
        category: 'cars',
        status: 'REMOVED',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();
    expect(shallowToJson(wrapper.find('Link[children="Опубликовать"]'))).not.toBeNull();
});

it('при клике на кнопку "Опубликовать" вызовет экшен для активации объявы', () => {
    offerActivate.mockImplementationOnce(() => () => Promise.resolve());
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: false, activate: true },
        category: 'cars',
        status: 'REMOVED',
        saleId: '1085562758-1970f439',
    };

    const wrapper = shallow(
        <CardOwnerControls offer={ offer }/>,
        { context: { ...contextMock, store } },
    ).dive().dive();

    const publishLink = wrapper.find('Link[children="Опубликовать"]');
    publishLink.simulate('click');

    expect(offerActivate).toHaveBeenCalledTimes(1);
    expect(offerActivate).toHaveBeenCalledWith({ category: 'cars', from: 'card-vas', offerIdHash: '1085562758-1970f439' });
});

describe('при клике на кнопку "подключить автопродление размещения"', () => {
    const offer = {
        additional_info: { is_owner: true, expire_date: 123 },
        actions: { edit: true, hide: true },
        category: 'cars',
        status: 'ACTIVE',
    };

    beforeEach(() => {
        contextMock.metrika.sendParams.mockClear();
    });

    it('вызовет необходимый экшен', () => {
        toggleAutoProlongation.mockImplementationOnce(jest.fn(() => () => Promise.resolve()));
        const wrapper = shallow(
            <CardOwnerControls offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive().dive();

        const placementAutoProlongationMessageBlock = wrapper.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        expect(toggleAutoProlongation).toHaveBeenCalledTimes(1);
        expect(toggleAutoProlongation).toHaveBeenCalledWith(
            true,
            { product: 'all_sale_activate' },
            { exposeError: true },
        );
    });

    it('если действие было удачным отправит соответствующую метрику', () => {
        const pr = Promise.resolve();
        toggleAutoProlongation.mockImplementationOnce(jest.fn(() => () => pr));
        const wrapper = shallow(
            <CardOwnerControls offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive().dive();

        const placementAutoProlongationMessageBlock = wrapper.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        return pr
            .then(() => {
                expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'prolongation-turn-on', 'from-offer' ]);
            });
    });

    it('если действие было неудачным отправит соответствующую метрику', () => {
        const pr = Promise.reject();
        toggleAutoProlongation.mockImplementationOnce(jest.fn(() => () => pr));
        const wrapper = shallow(
            <CardOwnerControls offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive().dive();

        const placementAutoProlongationMessageBlock = wrapper.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        return pr.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => setTimeout(resolve));
                expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'prolongation-errors' ]);
            },
        );
    });
});

describe('если у неактивного оффера есть продление со скидкой', () => {
    let wrapper;

    beforeEach(() => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withAction({ activate: true })
            .withSellerTypePrivate()
            .withStatus('INACTIVE')
            .withCustomVas({
                service: 'all_sale_activate',
                price: 999,
                original_price: 1777,
                days: 7,
                prolongation_forced_not_togglable: true,
                prolongation_interval_will_expire: '2020-03-18T13:55:33Z',
            })
            .value();
        MockDate.set('2020-03-18T13:00:00Z');
        //@see https://github.com/facebook/jest/issues/11551
        jest.useFakeTimers('legacy');

        wrapper = shallow(
            <CardOwnerControls offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive().dive();
    });

    it('при клике на кнопку откроет модал оплаты', () => {
        const discountBlock = wrapper.find('PlacementAutoProlongationExpireNotice');
        discountBlock.simulate('activateButtonClick', 'foo');

        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpen).toHaveBeenCalledWith('foo');
    });

    it('когда выйдет таймер обновит оффер', () => {
        const discountBlock = wrapper.find('PlacementAutoProlongationExpireNotice');
        discountBlock.simulate('timerFinish');

        expect(updateOffer).toHaveBeenCalledTimes(0);

        jest.advanceTimersByTime(3 * SECOND);

        expect(updateOffer).toHaveBeenCalledTimes(1);
        expect(updateOffer).toHaveBeenCalledWith({ category: 'cars', offerID: '1085562758-1970f439' });
    });
});
