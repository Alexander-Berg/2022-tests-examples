/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');
jest.mock('../../../actions/offer', () => ({
    activateOffer: jest.fn(() => () => {}),
}));
jest.mock('auto-core/react/dataDomain/sales/actions/fetchOffer', () => ({
    'default': jest.fn(() => () => {}),
}));
jest.mock('auto-core/react/dataDomain/sales/actions/toggleServiceAutoProlongation', () => ({
    fetchAndToggleServiceAutoProlongation: jest.fn(() => () => {}),
}));

const _ = require('lodash');
const React = require('react');
const SalesItem = require('./SalesItem');
const Vas = require('../../Vas');
const SalesItemGraph = require('../SalesItemGraph');
const SalesItemPrice = require('../SalesItemPrice');
const MockDate = require('mockdate');

const offerActions = require('../../../actions/offer');
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const stateMock = require('autoru-frontend/mockData/state/state.mock');
const offerStatsMock = require('autoru-frontend/mockData/state/offerStats.mock');
const moderationStatusMock = require('autoru-frontend/mockData/state/moderationStatus.mock');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { SECOND } = require('auto-core/lib/consts');
const fetchOffer = require('auto-core/react/dataDomain/sales/actions/fetchOffer').default;
const { fetchAndToggleServiceAutoProlongation } = require('auto-core/react/dataDomain/sales/actions/toggleServiceAutoProlongation');

let initialState;
let props;

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

let context;
let vasLogParams;
beforeEach(() => {
    initialState = {
        state: {
            paymentModalResult: _.cloneDeep(stateMock.paymentModalResult),
        },
        user: _.cloneDeep(userMock),
        offerStats: _.cloneDeep(offerStatsMock),
        sales: {
            state: {
                [cardMock.saleId]: {
                    unfolded: true,
                },
            },
        },
        moderationStatus: _.cloneDeep(moderationStatusMock),
        bunker: getBunkerMock([ 'common/vas_vip', 'common/activate_in_app' ]),
    };

    props = {
        offer: _.cloneDeep(cardMock),
    };

    context = _.cloneDeep(contextMock);

    context.logVasEvent = jest.fn((params) => {
        vasLogParams = params;
    });

    context.link = jest.fn((route, params) => `link to "${ route }" with params: ${ JSON.stringify(params) }`);

    paymentModalOpen.mockClear();
    context.logVasEvent.mockClear();
    context.metrika.sendParams.mockClear();
});

afterEach(() => {
    MockDate.reset();
});

describe('активация объявления', () => {
    beforeEach(() => {
        initialState.sales.state[props.offer.saleId].unfolded = false;
        props.offer.actions.activate = true;
        props.offer.status = 'INACTIVE';

        offerActions.activateOffer.mockImplementation(() => {
            return () => {
                return Promise.resolve();
            };
        });
    });

    it('должен задисейблить кнопку после клика', () => {
        const wrapper = shallowRenderComponent();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        expect(wrapper.find('.SalesItem__buttonActivate')).toHaveProp('disabled', true);
    });

    it('должен отправить метрику после клика', () => {
        const wrapper = shallowRenderComponent();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'clicks', 'activate' ]);
    });

    it('должен раздисейблить кнопку после успешного ответа', async() => {
        const result = offerActions.activateOffer.mockImplementation(() => {
            return () => Promise.resolve();
        });

        const wrapper = shallowRenderComponent();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        await result;
        await Promise.resolve();
        expect(wrapper.find('.SalesItem__buttonActivate')).toHaveProp('disabled', false);
    });

    it('должен раздисейблить кнопку после неуспешного ответа', async() => {
        const result = offerActions.activateOffer.mockImplementation(() => {
            return () => Promise.reject();
        });

        const wrapper = shallowRenderComponent();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        try {
            await result;
        } catch (e) {}
        await Promise.resolve();
        expect(wrapper.find('.SalesItem__buttonActivate')).toHaveProp('disabled', false);
    });

    it('должен показать переданное сообщение после неуспешного ответа', async() => {
        const promise = Promise.reject({ message: 'В объявлении не хватает телефона' });
        offerActions.activateOffer.mockImplementation(() => {
            return () => promise;
        });

        const { store, wrapper } = shallowRenderComponentAndStore();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        try {
            await promise;
        } catch (e) {}
        expect(store.getActions()).toEqual([
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'В объявлении не хватает телефона', view: 'error' },
            },
        ]);
    });

    it('должен показать дефолтное сообщение после неуспешного ответа', async() => {
        const promise = Promise.reject();
        offerActions.activateOffer.mockImplementation(() => {
            return () => promise;
        });

        const { store, wrapper } = shallowRenderComponentAndStore();
        wrapper.find('.SalesItem__buttonActivate').simulate('click');

        try {
            await promise;
        } catch (e) {}
        expect(store.getActions()).toEqual([
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Произошла ошибка', view: 'error' },
            },
        ]);
    });
});

describe('информация о банах', () => {
    it('должен отрендерить информацию о причинах бана', () => {
        initialState.sales.state[props.offer.saleId].unfolded = false;
        props.offer.human_reasons_ban = [ { text: 'текст' } ];
        props.offer.status = 'BANNED';

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__infoMessages')).toHaveHTML(`
            <div class="SalesItem__infoMessages">
                <div class="SalesItem__infoMessagesReason">
                    <div class="SalesItem__infoMessagesReasonText">текст</div>
                </div>
            </div>
        `.trim());
    });

    it('не должен отрендерить информацию, если ее нет', () => {
        initialState.sales.state[props.offer.saleId].unfolded = false;
        props.offer.status = 'BANNED';

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__infoMessages')).toHaveLength(0);
    });
});

describe('информация о статусе', () => {
    it('должен отрендерить плашку "на модерации"', () => {
        initialState.sales.state[props.offer.saleId].unfolded = false;
        props.offer.additional_info.is_on_moderation = true;
        props.offer.status = 'BANNED';

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__status')).toMatchElement(
            <div className="SalesItem__status">
                <div className="SalesItem__warning SalesItem__warning_green">На модерации</div>
                <div className="SalesItem__warning">Заблокировано модератором</div>
            </div>,
        );
    });
});

describe('пробег/моточасы', () => {
    it('должен отрендерить моточасы, если они есть', () => {
        initialState.sales.state[props.offer.saleId].unfolded = true;
        props.offer.category = 'trucks';
        props.offer.section = 'used';
        props.offer.state.mileage = 100000;
        props.offer.status = 'ACTIVE';
        props.offer.sub_category = 'crane';
        props.offer.vehicle_info.operating_hours = 13000;

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__infoColumn_kmAge').text()).toEqual('13\u00a0000 моточасов');
    });

    it('должен отрендерить пробег, если он есть', () => {
        initialState.sales.state[props.offer.saleId].unfolded = true;
        props.offer.category = 'cars';
        props.offer.section = 'used';
        props.offer.state.mileage = 100000;
        props.offer.status = 'ACTIVE';
        props.offer.sub_category = 'cars';

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__infoColumn_kmAge').text()).toEqual('100\u00a0000 км');
    });

    it('должен отрендерить прочерка, если нет ни моточасов, ни пробега', () => {
        initialState.sales.state[props.offer.saleId].unfolded = true;
        props.offer.category = 'cars';
        props.offer.section = 'used';
        delete props.offer.state.mileage;
        props.offer.status = 'ACTIVE';
        props.offer.sub_category = 'cars';

        const wrapper = shallowRenderComponent(props, context, initialState);

        expect(wrapper.find('.SalesItem__infoColumn_kmAge').text()).toEqual('—');
    });
});

describe('при клике на кнопку "оплатить" в блоке васов', () => {
    beforeEach(() => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withCustomVas({ service: 'all_sale_special', price: 500, original_price: undefined })
            .withCustomVas({ service: 'all_sale_toplist', price: 350, original_price: undefined })
            .value();

        const page = shallowRenderComponent({ ...props, offer }, context, initialState);
        const vasBlock = page.find(Vas);
        vasBlock.simulate('submit', [ 'all_sale_special', 'all_sale_toplist' ]);
    });

    it('залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(2);
        expect(context.logVasEvent.mock.calls[0][0]).toMatchSnapshot();
        expect(context.logVasEvent.mock.calls[1][0]).toMatchSnapshot();
    });

    it('откроет окно оплаты с правильными параметрами когда нет скидки', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });

    it('откроет модал с параметрами shouldUpdateOfferAfter=false и shouldUpdateUserOffersAfter=true, если есть скидка на ВАС', () => {
        const offer = cloneOfferWithHelpers(cardMock)
            .withActiveVas([])
            .value();

        const page = shallowRenderComponent({
            ...props,
            offer,
        });

        page.find(Vas).simulate('click');

        expect(paymentModalParams.shouldUpdateOfferAfter).toEqual(false);
        expect(paymentModalParams.shouldUpdateUserOffersAfter).toEqual(true);
    });
});

describe('при клике на кнопку "поднятие в поиске"', () => {
    beforeEach(() => {
        const page = shallowRenderComponent(props, context, initialState);
        const graphBlock = page.find(SalesItemGraph);
        graphBlock.simulate('vasSubmit', [ 'all_sale_fresh' ]);
    });

    it('залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('откроет окно оплаты с правильными параметрами', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });
});

describe('при клике на кнопку "выделение цветом"', () => {
    beforeEach(() => {
        const page = shallowRenderComponent(props, context, initialState);
        const priceBlock = page.find(SalesItemPrice);
        priceBlock.simulate('submit', [ 'all_sale_color' ]);
    });

    it('залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('откроет окно оплаты с правильными параметрами', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });
});

it('если есть автопродляемое размещение покажет расширенную информацию', () => {
    props.offer.service_prices = [ { service: 'all_sale_activate', prolongation_forced_not_togglable: true } ];
    props.offer.additional_info.expire_date = '111';

    const page = shallowRenderComponent(props, context, initialState);

    expect(page.find('PlacementProlongationInfoLk')).toHaveLength(1);
});

describe('при клике на кнопку "подключить автопродление размещения"', () => {
    it('вызовет необходимый экшен', () => {
        fetchAndToggleServiceAutoProlongation.mockImplementationOnce(jest.fn(() => () => Promise.resolve()));
        const page = shallowRenderComponent(props, context, initialState);

        const placementAutoProlongationMessageBlock = page.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        expect(fetchAndToggleServiceAutoProlongation).toHaveBeenCalledTimes(1);
        expect(fetchAndToggleServiceAutoProlongation).toHaveBeenCalledWith(
            true,
            { category: 'cars', offerId: '1085562758-1970f439', product: 'all_sale_activate' },
            { exposeError: true },
        );
    });

    it('если действие было удачным отправит соответствующую метрику', () => {
        const pr = Promise.resolve();
        fetchAndToggleServiceAutoProlongation.mockImplementationOnce(jest.fn(() => () => pr));
        const page = shallowRenderComponent(props, context, initialState);

        const placementAutoProlongationMessageBlock = page.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        return pr
            .then(() => {
                expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                expect(context.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'prolongation-turn-on', 'from-lk' ]);
            });
    });

    it('если действие было неудачным отправит соответствующую метрику', () => {
        const pr = Promise.reject();
        fetchAndToggleServiceAutoProlongation.mockImplementationOnce(jest.fn(() => () => pr));
        const page = shallowRenderComponent(props, context, initialState);

        const placementAutoProlongationMessageBlock = page.find('PlacementAutoProlongationInactiveNotice');
        placementAutoProlongationMessageBlock.simulate('prolongationButtonClick');

        return pr.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => setTimeout(resolve));
                expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
                expect(context.metrika.sendParams).toHaveBeenCalledWith([ '7days-placement', 'prolongation-errors' ]);
            },
        );
    });
});

describe('если у неактивного оффера есть продление со скидкой', () => {
    let page;

    beforeEach(() => {
        props.offer = cloneOfferWithHelpers(cardMock)
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
        initialState.sales.state = {};
        MockDate.set('2020-03-18T13:00:00Z');
        //@see https://github.com/facebook/jest/issues/11551
        jest.useFakeTimers('legacy');

        page = shallowRenderComponent(props, context, initialState);
    });

    it('при клике на кнопку откроет модал оплаты', () => {
        const discountBlock = page.find('PlacementAutoProlongationExpireNotice');
        discountBlock.simulate('activateButtonClick', 'foo');

        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalOpen).toHaveBeenCalledWith('foo');
    });

    it('когда выйдет таймер обновит оффер', () => {
        const discountBlock = page.find('PlacementAutoProlongationExpireNotice');
        discountBlock.simulate('timerFinish');

        expect(fetchOffer).toHaveBeenCalledTimes(0);

        jest.advanceTimersByTime(3 * SECOND);

        expect(fetchOffer).toHaveBeenCalledTimes(1);
        expect(fetchOffer).toHaveBeenCalledWith({ category: 'cars', offerID: '1085562758-1970f439' });
    });

    it('когда выйдет таймер покажет кнопку "активировать"', () => {
        expect(page.find('Button[children="Активировать"]').isEmptyRender()).toBe(true);

        const discountBlock = page.find('PlacementAutoProlongationExpireNotice');
        discountBlock.simulate('timerFinish');
        jest.advanceTimersByTime(3 * SECOND);

        expect(page.find('Button[children="Активировать"]').isEmptyRender()).toBe(false);
    });
});

function shallowRenderComponent() {
    return shallowRenderComponentAndStore().wrapper;
}

function shallowRenderComponentAndStore() {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <SalesItem { ...props } store={ store }/>
        </ContextProvider>,
    );

    return {
        wrapper: wrapper.dive().dive().dive(),
        store: store,
    };
}
