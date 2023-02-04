/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const bunkerMock = getBunkerMock([ 'common/vas', 'common/vas_vip' ]);
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const userMock = require('auto-core/react/dataDomain/user/mocks').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const _ = require('lodash');
const React = require('react');
const CardVAS = require('./CardVAS');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const paymentModalOpen = require('auto-core/react/dataDomain/state/actions/paymentModalOpen');

let paymentModalParams;
paymentModalOpen.mockImplementation((params) => {
    paymentModalParams = params;
    return jest.fn();
});

let vasLogParams;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

let initialState;
let props;
let store;
let context;

beforeEach(() => {
    const offer = cloneOfferWithHelpers(cardMock).withIsOwner().value();
    const config = configStateMock.withPageType('card').value();

    initialState = {
        bunker: _.cloneDeep(bunkerMock),
        user: userMock.withAuth(true).value(),
        config,
    };

    props = {
        offer: offer,
        // я хз на зуя это еще раз передавать как пропс, но создателем спорить не буду
        activeServices: offer.services,
        services: offer.service_prices,
    };

    context = _.cloneDeep(contextMock);

    context.logVasEvent.mockClear();
});

describe('ничего не нарисует и не отправит метрику', () => {
    it('если это не карточка хозяина', () => {
        props.offer = cloneOfferWithHelpers(cardMock).withIsOwner(false).value();
        const page = shallowRenderComponent();
        expect(page.html()).toBeNull();
        expect(context.logVasEvent).not.toHaveBeenCalled();
    });

    it('если оффер неактивен', () => {
        props.offer.status = 'FOO';
        const page = shallowRenderComponent();
        expect(page.html()).toBeNull();
        expect(context.logVasEvent).not.toHaveBeenCalled();
    });

    it('если оффер принадлежит дилеру', () => {
        props.offer.seller_type = 'COMMERCIAL';
        const page = shallowRenderComponent();
        expect(page.html()).toBeNull();
        expect(context.logVasEvent).not.toHaveBeenCalled();
    });

    it('если куплен пакет вип', () => {
        props.activeServices = [ { service: 'package_vip', is_active: true } ];
        const page = shallowRenderComponent();
        expect(page.html()).toBeNull();
        expect(context.logVasEvent).not.toHaveBeenCalled();
    });
});

it('при рендере отправит метрику показов васов для первой вкладки', () => {
    props.activeServices = [];
    context.pageParams.category = 'cars';
    // убеждаемся что у объявы нет випа
    props.offer = cloneOfferWithHelpers(cardMock)
        .withPrice(initialState.bunker['common/vas_vip'].minvalue - 1)
        .withCustomVas({ service: 'package_vip', recommendation_priority: 0 })
        .value();

    shallowRenderComponent();

    expect(context.logVasEvent).toHaveBeenCalledTimes(1);
    expect(vasLogParams).toMatchSnapshot();
});

it('при смене вкладки правильно отправит метрику показов', () => {
    props.activeServices = [];
    context.pageParams.category = 'cars';

    const page = shallowRenderComponent();
    const turboTab = page.find('.CardVAS__tab_package_turbo');
    turboTab.simulate('mouseOver');

    expect(context.logVasEvent).toHaveBeenCalledTimes(2);
    expect(vasLogParams).toMatchSnapshot();
});

describe('скидка на пакет услуг', () => {
    it('если есть скидка, отобразит её на кнопке', () => {
        props.activeServices = [];
        const page = shallowRenderComponent();
        const buttonSubText = page.find('.CardVAS__buttonSubText');

        expect(shallowToJson(buttonSubText)).toMatchSnapshot();
    });

    it('если скидки нет (или цена больше предыдущей), ничего не нарисует', () => {
        props.activeServices = [];
        toggleDiscount(false, 'package_vip');
        const page = shallowRenderComponent();
        const buttonSubText = page.find('.CardVAS__buttonSubText');

        expect(buttonSubText).toHaveLength(0);
    });
});

describe('кнопка на вкладке "поднятия в поиске"', () => {
    it('если есть скидка и включено автоподнятие, напишет об автоподнятие', () => {
        toggleDiscount(true, 'all_sale_fresh');
        props.offer = cloneOfferWithHelpers(cardMock).withServiceSchedule('all_sale_fresh').value();

        const page = shallowRenderComponent();
        selectTab(page, 'all_sale_fresh');
        const buttonContent = page.find('.CardVAS__buttonContent');

        expect(shallowToJson(buttonContent)).toMatchSnapshot();
    });

    it('если есть скидка, автоподнятие выключено, но услуга была подключена ранее, напишет как давно была подключена услуга', () => {
        toggleDiscount(true, 'all_sale_fresh');
        props.activeServices = [ { service: 'all_sale_fresh', is_active: true, create_date: '1559563125517' } ];

        const page = shallowRenderComponent();
        selectTab(page, 'all_sale_fresh');
        const buttonContent = page.find('.CardVAS__buttonContent');

        expect(shallowToJson(buttonContent)).toMatchSnapshot();
    });

    it('если есть скидка и поднятие не было подключено ранее, нарисует скидку', () => {
        toggleDiscount(true, 'all_sale_fresh');

        const page = shallowRenderComponent();
        selectTab(page, 'all_sale_fresh');
        const buttonContent = page.find('.CardVAS__buttonContent');

        expect(shallowToJson(buttonContent)).toMatchSnapshot();
    });

    it('если ничего такого нет, просто напишет цену услуги', () => {
        toggleDiscount(false, 'all_sale_fresh');

        const page = shallowRenderComponent();
        selectTab(page, 'all_sale_fresh');
        const buttonContent = page.find('.CardVAS__buttonContent');

        expect(shallowToJson(buttonContent)).toMatchSnapshot();
    });
});

describe('при клике на кнопку', () => {
    beforeEach(() => {
        const page = shallowRenderComponent();
        const button = page.find('Button');
        button.simulate('click');
    });

    it('передаст правильные параметры в модал оплаты', () => {
        expect(paymentModalOpen).toHaveBeenCalledTimes(1);
        expect(paymentModalParams).toMatchSnapshot();
    });

    it('правильно залогирует событие клика', () => {
        expect(context.logVasEvent).toHaveBeenCalledTimes(2);
        expect(vasLogParams).toMatchSnapshot();
    });
});

function shallowRenderComponent() {
    store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <CardVAS { ...props } store={ store }/>
        </ContextProvider>,
    );
    return wrapper.dive().dive();
}

function selectTab(page, service) {
    const freshTab = page.find(`.CardVAS__tab_${ service }`);
    freshTab.simulate('mouseOver');
}

function toggleDiscount(isOn, service, discountValue = 40) {
    function getOriginalPrice(price) {
        return isOn ? Math.floor(price / (100 - discountValue) * 100) : price - 1;
    }

    props.services = props.services.map((s) => {
        return s.service === service ?
            { ...s, original_price: getOriginalPrice(s.price) } :
            s;
    });
}
