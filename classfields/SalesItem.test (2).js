/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/sales/actions/fetchOffer');
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('auto-core/react/dataDomain/drafts/actions/deleteDraft', () => ({
    'default': jest.fn(),
}));

const React = require('react');
const { shallow } = require('enzyme');
const MockDate = require('mockdate');

const SalesItem = require('./SalesItem');
const fetchOffer = require('auto-core/react/dataDomain/sales/actions/fetchOffer').default;
fetchOffer.mockImplementation(() => () => {});

const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');
paymentModalOpen.mockImplementation(() => () => {});

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardStateMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const geoStateMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const { SECOND } = require('auto-core/lib/consts');
const deleteDraftMock = require('auto-core/react/dataDomain/drafts/actions/deleteDraft').default;

let props;
let initialState;
const { location } = global;

beforeEach(() => {
    initialState = {
        geo: geoStateMock,
        sales: {
            state: {},
        },
        bunker: getBunkerMock([ 'common/vas_vip', 'common/activate_in_app' ]),
        user: { data: {} },
    };
    props = {
        offer: cardStateMock,
    };

    delete global.location;
    global.location = {
        assign: jest.fn(),
    };

    jest.spyOn(global, 'confirm').mockImplementation(() => { });
});

afterEach(() => {
    jest.restoreAllMocks();
    MockDate.reset();
    global.location = location;
});

describe('блок продления размещения со скидкой:', () => {
    describe('если скидка не действует для объявления', () => {
        beforeEach(() => {
            props.offer = cloneOfferWithHelpers(cardStateMock)
                .withExpireDate('2020-03-18T00:00:00Z')
                .withStatus('INACTIVE')
                .withAction({
                    activate: true,
                })
                .withCustomVas({
                    service: 'all_sale_activate',
                    price: 999,
                    original_price: 1777,
                    days: 7,
                    prolongation_forced_not_togglable: true,
                })
                .value();

            MockDate.set('2020-03-18T13:00:00Z');
        });

        it('не отправит вас лог при маунте компонента', () => {
            shallowRenderComponent({ initialState, props });

            expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);
        });

        it('не нарисует блок про скидку на продление размещения', () => {
            const page = shallowRenderComponent({ initialState, props });
            const placementProlongationDiscountBlock = page.find('PlacementProlongationDiscount');

            expect(placementProlongationDiscountBlock.isEmptyRender()).toBe(true);
        });
    });

    describe('если скидка действует для объявления', () => {
        beforeEach(() => {
            props.offer = cloneOfferWithHelpers(cardStateMock)
                .withExpireDate('2020-03-18T00:00:00Z')
                .withStatus('INACTIVE')
                .withAction({
                    activate: true,
                })
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
        });

        it('отправит вас лог при маунте компонента', () => {
            shallowRenderComponent({ initialState, props });

            expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
        });

        it('нарисует блок про скидку на продление размещения', () => {
            const page = shallowRenderComponent({ initialState, props });
            const placementProlongationDiscountBlock = page.find('PlacementProlongationDiscount');

            expect(placementProlongationDiscountBlock.isEmptyRender()).toBe(false);
        });

        it('при клике на кнопку активировать создаст действие на открытие модала оплаты', () => {
            const page = shallowRenderComponent({ initialState, props });
            const button = page.find('FormButton').findWhere((n) => n.prop('color') === 'green');
            button.simulate('click');

            expect(paymentModalOpen).toHaveBeenCalledTimes(1);
            expect(paymentModalOpen.mock.calls[0]).toMatchSnapshot();
        });

        it('при клике на кнопку активировать отправит вас лог', () => {
            const page = shallowRenderComponent({ initialState, props });
            const button = page.find('FormButton').findWhere((n) => n.prop('color') === 'green');
            button.simulate('click');

            expect(contextMock.logVasEvent).toHaveBeenCalledTimes(2);
            expect(contextMock.logVasEvent.mock.calls[1]).toMatchSnapshot();
        });

        it('когда таймер выйдет обновит оффер и покажет обычную кнопку "активировать"', () => {
            //@see https://github.com/facebook/jest/issues/11551
            jest.useFakeTimers('legacy');

            const page = shallowRenderComponent({ initialState, props });
            const placementProlongationDiscountBlock = page.find('PlacementProlongationDiscount');
            placementProlongationDiscountBlock.simulate('timerFinish');

            expect(page.find('Button[children="Активировать"]').isEmptyRender()).toBe(true);
            expect(fetchOffer).toHaveBeenCalledTimes(0);

            jest.advanceTimersByTime(3 * SECOND);

            expect(fetchOffer).toHaveBeenCalledTimes(1);
            expect(fetchOffer).toHaveBeenCalledWith({ category: 'cars', offerID: '1085562758-1970f439' });
        });
    });
});

describe('для драфта', () => {
    beforeEach(() => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withStatus('DRAFT')
            .value();
    });

    it('формирует правильную ссылку на тайтле', () => {
        const page = shallowRenderComponent({ initialState, props });
        const titleLink = page.find('.SalesItem__link');
        expect(titleLink.prop('url')).toBe('link/fromWebToApp/?action=edit');
        expect(titleLink.prop('metrika')).toBe('draft_in_lk,click');
    });

    it('добавляет кнопки редактирования и удаления', () => {
        const page = shallowRenderComponent({ initialState, props });
        const buttons = page.find('.SalesItem__button');
        const editButton = buttons.at(0);

        expect(editButton.prop('url')).toBe('link/fromWebToApp/?action=edit');
        expect(buttons).toMatchSnapshot();
    });

    it('правильно обрабатывает клик по тумбе', () => {
        const page = shallowRenderComponent({ initialState, props });
        const thumb = page.find('.SalesItem__img');
        thumb.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'draft_in_lk', 'click' ]);

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith('link/fromWebToApp/?action=edit');
    });

    describe('удаление:', () => {
        it('без подтверждения не будет удалять драфт', () => {
            global.confirm.mockImplementationOnce(() => false);

            const page = shallowRenderComponent({ initialState, props });
            const deleteButton = page.find('.SalesItem__button').at(1);
            deleteButton.simulate('click');

            expect(global.confirm).toHaveBeenCalledTimes(1);
            expect(deleteDraftMock).toHaveBeenCalledTimes(0);
        });

        it('при клике на кнопку удалить отправит запрос', () => {
            deleteDraftMock.mockImplementationOnce(() => () => Promise.resolve());
            global.confirm.mockImplementationOnce(() => true);

            const page = shallowRenderComponent({ initialState, props });
            const deleteButton = page.find('.SalesItem__button').at(1);
            deleteButton.simulate('click');

            expect(global.confirm).toHaveBeenCalledTimes(1);
            expect(deleteDraftMock).toHaveBeenCalledTimes(1);
            expect(deleteDraftMock).toHaveBeenCalledWith({ parent_category: 'cars', draft_id: '1085562758-1970f439' });
        });

        it('после удачного запроса на удаление отправит метрику', () => {
            const draftDeletePromise = Promise.resolve();
            deleteDraftMock.mockImplementationOnce(() => () => draftDeletePromise);
            global.confirm.mockImplementationOnce(() => true);

            const page = shallowRenderComponent({ initialState, props });
            contextMock.metrika.sendPageEvent.mockClear();
            const deleteButton = page.find('.SalesItem__button').at(1);
            deleteButton.simulate('click');

            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
            return draftDeletePromise
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'draft_in_lk', 'delete' ]);
                });
        });

        it('после неудачного запроса на удаление не отправит метрику', () => {
            const draftDeletePromise = Promise.reject();
            deleteDraftMock.mockImplementationOnce(() => () => draftDeletePromise);
            global.confirm.mockImplementationOnce(() => true);

            const page = shallowRenderComponent({ initialState, props });
            contextMock.metrika.sendPageEvent.mockClear();
            const deleteButton = page.find('.SalesItem__button').at(1);
            deleteButton.simulate('click');

            return draftDeletePromise
                .catch(() => { })
                .then(() => {
                    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
                });
        });
    });
});

describe('эксп AUTORUFRONT-21494_mobile_poffer', () => {
    describe('драфт', () => {
        beforeEach(() => {
            props.offer = cloneOfferWithHelpers(cardStateMock)
                .withStatus('DRAFT')
                .value();
            contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-21494_mobile_poffer');
        });

        afterEach(() => {
            contextMock.hasExperiment.mockReset();
        });

        it('формирует правильную ссылку на тайтле', () => {
            const page = shallowRenderComponent({ initialState, props });
            const titleLink = page.find('.SalesItem__link');
            expect(titleLink.prop('url')).toBe('linkDesktop/form/?category=cars&section=used&form_type=add&from_lk=true');
            expect(titleLink.prop('metrika')).toBe('draft_in_lk,click');
        });

        it('формирует правильную ссылку при клике по тумбе', () => {
            const page = shallowRenderComponent({ initialState, props });
            const thumb = page.find('.SalesItem__img');
            thumb.simulate('click');

            expect(global.location.assign).toHaveBeenCalledTimes(1);
            expect(global.location.assign).toHaveBeenCalledWith('linkDesktop/form/?category=cars&section=used&form_type=add&from_lk=true');
        });

        it('формирует правильную ссылку при клике на кнопку Редактировать', () => {
            const page = shallowRenderComponent({ initialState, props });
            const editButton = page.find('.SalesItem__button').at(0);

            expect(editButton.prop('url')).toBe('linkDesktop/form/?category=cars&section=used&form_type=add&from_lk=true');
        });
    });
    describe('активный', () => {
        beforeEach(() => {
            props.offer = cloneOfferWithHelpers(cardStateMock).value();

            contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-21494_mobile_poffer');
        });

        afterEach(() => {
            contextMock.hasExperiment.mockReset();
        });

        it('формирует правильную ссылку при клике на кнопку Редактировать', () => {
            const page = shallowRenderComponent({ initialState, props });
            const editButton = page.find('.SalesItem__button').at(0);

            expect(editButton.prop('url')).toBe('linkDesktop/form/?category=cars&section=used&form_type=edit&sale_id=1085562758&sale_hash=1970f439');
        });
    });
});

describe('кнопка активации оффера', () => {
    beforeEach(() => {
        MockDate.set('2020-03-18T13:00:00Z');
    });

    it('активировать', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withStatus('INACTIVE')
            .withAction({
                activate: true,
            })
            .value();

        const page = shallowRenderComponent({ initialState, props });
        const button = page.find('FormButton').at(0);

        expect(button.dive().shallow().text()).toEqual('Активировать');
    });

    it('активировать сейчас, если запланирована дата активации', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withReactivationDate('2020-03-20T00:00:00Z')
            .withStatus('INACTIVE')
            .withAction({
                activate: true,
            })
            .value();

        const page = shallowRenderComponent({ initialState, props });
        const button = page.find('FormButton').at(0);

        expect(button.dive().shallow().text()).toEqual('Активировать сейчас');
    });
});

function shallowRenderComponent({ initialState, props }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <SalesItem { ...props } store={ store }/>
        </ContextProvider>,
    ).dive().dive().dive();
}
