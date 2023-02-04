/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const _ = require('lodash');
const BillingPaymentMethods = require('./BillingPaymentMethods');
const { mount, shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { formatTiedCardInfo, methods } = require('autoru-frontend/mockData/responses/billing/paymentMethods.mock');
const { ERRORS } = require('auto-core/lib/billing/utils');

const DEFAULT_PROPS = {
    paymentMethods: [
        methods.bankCard,
        methods.sberbank,
        methods.yandexMoney,
        methods.qiwi,
        methods.webmoney,
    ],
    selectedMethodId: 'bank_card',
    canChangeMethod: true,
    isMobile: false,
    onMethodChange: () => {},
    error: {},
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисует компонент для десктопа', () => {
    props.isMobile = false;
    const wrapper = shallow(<BillingPaymentMethods { ...props }/>);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('ничего не нарисует если есть ошибка инициализации', () => {
    props.error = { type: ERRORS.init_fail };
    const wrapper = shallow(<BillingPaymentMethods { ...props }/>);
    expect(wrapper.isEmptyRender()).toBe(true);
});

it('ничего не нарисует если есть ошибка запроса транзакции', () => {
    props.error = { type: ERRORS.transaction_fetch_fail };
    const wrapper = shallow(<BillingPaymentMethods { ...props }/>);
    expect(wrapper.isEmptyRender()).toBe(true);
});

it('если передан только один метод то ничего не нарисует', () => {
    props.paymentMethods = props.paymentMethods.slice(0, 1);
    const wrapper = shallow(<BillingPaymentMethods { ...props }/>);
    expect(wrapper).toBeEmptyRender();
});

it('если передано меньше 4 методов то не нарисует тогглер и выпадашку', () => {
    props.paymentMethods = props.paymentMethods.slice(0, 3);
    const wrapper = shallow(<BillingPaymentMethods { ...props }/>);
    const dropDown = wrapper.find('.BillingPaymentMethods__tile_type_toggler');
    expect(dropDown).toHaveLength(0);
});

it('если есть привязанные карты то нарисует брэнд первой из них', () => {
    props.tiedCards = [
        formatTiedCardInfo(methods.newApiTiedCard),
        formatTiedCardInfo(methods.newApiTiedCard2),
    ];
    const bankCardTile = shallow(<BillingPaymentMethods { ...props }/>).find('.BillingPaymentMethods__tile').at(0);
    expect(shallowToJson(bankCardTile)).toMatchSnapshot();
});

it('если есть привязанные карты, но бренд не распарсили, не упадёт', () => {
    props.tiedCards = [
        formatTiedCardInfo(methods.newApiTiedCardWithoutBrand),
        formatTiedCardInfo(methods.newApiTiedCard2),
    ];
    const bankCardTile = shallow(<BillingPaymentMethods { ...props }/>).find('.BillingPaymentMethods__tile').at(0);
    expect(shallowToJson(bankCardTile)).toMatchSnapshot();
});

describe('при выборе метода передаст в коллбэк его id', () => {
    beforeEach(() => {
        props.onMethodChange = jest.fn();
    });

    it('для десктопа', () => {
        const INDEX = 1;
        const wrapper = mount(<BillingPaymentMethods { ...props }/>);
        const secondPaymentMethodTile = wrapper.update().find('.BillingPaymentMethods__tile').at(INDEX);

        secondPaymentMethodTile.simulate('click');

        expect(props.onMethodChange).toHaveBeenCalledTimes(1);
        expect(props.onMethodChange).toHaveBeenCalledWith(props.paymentMethods[INDEX].id);
    });
});

describe('если пользователь нажал на тогглер', () => {

    let wrapper;

    beforeEach(() => {
        wrapper = mount(<BillingPaymentMethods { ...props }/>);
        const toggler = wrapper.find('.BillingPaymentMethods__tile_type_toggler');
        toggler.simulate('click');
    });

    it('откроется выпадашка с остальными методами', () => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('при клике на второстепенный метод выпадашка закроется и тогглер будет содержать иконку и название выбранного метода', () => {
        const thirdPaymentMethodTile = wrapper.find('.BillingPaymentMethods__tile').at(3);

        thirdPaymentMethodTile.simulate('click');
        wrapper.setProps({ ...props, selectedMethodId: 'yandex_money' });

        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('при клике на второстепенный метод и последующем выборе основного метода тогглер сбросится в дефолтное состояние', () => {
        const secondPaymentMethodTile = wrapper.update().find('.BillingPaymentMethods__tile').at(1);
        const thirdPaymentMethodTile = wrapper.find('.BillingPaymentMethods__tile').at(3);
        thirdPaymentMethodTile.simulate('click');
        secondPaymentMethodTile.simulate('click');
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});

describe('если пользователь уже начал оплату', () => {
    let wrapper;

    beforeEach(() => {
        props.canChangeMethod = false;
        props.onMethodChange = jest.fn();
        props.onMethodChange.mockClear();
        wrapper = mount(<BillingPaymentMethods { ...props }/>);
    });

    it('не будет реагировать на клике на методах оплаты', () => {
        const secondPaymentMethodTile = wrapper.find('.BillingPaymentMethods__tile').at(1);
        secondPaymentMethodTile.simulate('click');

        expect(props.onMethodChange).not.toHaveBeenCalled();
    });

    it('будет реагировать на клик по тогглеру', () => {
        const toggler = wrapper.find('.BillingPaymentMethods__tile_type_toggler');
        toggler.simulate('click');

        const dropDown = wrapper.find('.BillingPaymentMethods__dropDown');

        expect(dropDown).toHaveLength(1);
    });
});

it('при ошибке платежа отреагирует на клик по методу оплаты', () => {
    props.onMethodChange = jest.fn();
    props.canChangeMethod = true;
    props.error = { type: ERRORS.payment_fail };
    const wrapper = mount(<BillingPaymentMethods { ...props }/>);

    const secondPaymentMethodTile = wrapper.find('.BillingPaymentMethods__tile').at(1);
    secondPaymentMethodTile.simulate('click');

    expect(props.onMethodChange).toHaveBeenCalledTimes(1);
});
