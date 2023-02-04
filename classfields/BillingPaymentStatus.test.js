/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const _ = require('lodash');
const BillingPaymentStatus = require('./BillingPaymentStatus');
const Button = require('auto-core/react/components/islands/Button');
const Select = require('auto-core/react/components/islands/Select');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { ERRORS, PAYMENT_STATUSES } = require('auto-core/lib/billing/utils');
const MockDate = require('mockdate');
const { SECOND } = require('auto-core/lib/consts');

const DEFAULT_PROPS = {
    status: PAYMENT_STATUSES.closed,
    autoProlongableService: undefined,
    autoProlongationChecked: false,
    onAutoProlongationButtonClick: () => { },
    onAutoBoostButtonClick: () => { },
    isMobile: false,
    error: {},
    hasSupportLinkOnFailScreen: true,
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
    MockDate.set('2019-02-26 10:10:10');
});

afterEach(() => {
    MockDate.reset();
});

it('если это новый платеж то ничего не рисует', () => {
    const tree = shallow(<BillingPaymentStatus { ...props }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('если платеж обрабатывается то рисует лоадер', () => {
    props.status = PAYMENT_STATUSES.process;
    const tree = shallow(<BillingPaymentStatus { ...props }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});

describe('если платеж не удался', () => {
    let wrapper;
    let originalWindowParent;

    beforeEach(() => {
        props.status = PAYMENT_STATUSES.failed;
        props.error = { type: ERRORS.payment_fail };
        wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        originalWindowParent = global.parent;
        global.parent.postMessage = jest.fn();
    });

    afterEach(() => {
        global.parent = originalWindowParent;
    });

    it('нарисует экран с крестиком и ссылкой на чат', () => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('при клике на ссылку вызовет чат с тех поддержкой', () => {
        const chatLink = wrapper.find('.BillingPaymentStatus__supportLink');
        chatLink.simulate('click');
        expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
    });
});

describe('при успешной оплате', () => {
    beforeEach(() => {
        props.status = PAYMENT_STATUSES.paid;
    });

    it('покажет обычный экран если нет автопродляемых опций', () => {
        const wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('покажет обычный экран если это мобилка', () => {
        props.isMobile = true;
        props.autoProlongableService = { service: 'package_turbo' };

        const wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('покажет обычный экран если есть автопродляемая опция но она была выбрана до оплаты', () => {
        props.autoProlongableService = { service: 'package_turbo' };
        props.autoProlongationChecked = true;
        const wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    describe('если есть автопродление после оплаты', () => {
        let wrapper;

        beforeEach(() => {
            props.autoProlongableService = { service: 'package_turbo', days: 3, base_price: 999 };
            props.autoProlongationChecked = false;
            props.onAutoProlongationButtonClick = jest.fn();
            wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        });

        it('покажет картинку и кнопку включения', () => {
            expect(shallowToJson(wrapper)).toMatchSnapshot();
        });

        it('при клике на кнопку вызовет коллбэк из пропсов', () => {
            const autoProlongationButton = wrapper.find(Button);
            autoProlongationButton.simulate('click');
            const autoProlongationButtonUpdated = wrapper.find(Button);

            expect(autoProlongationButtonUpdated.prop('disabled')).toBe(true);
            expect(props.onAutoProlongationButtonClick).toHaveBeenCalledTimes(1);
            expect(props.onAutoProlongationButtonClick).toHaveBeenCalledWith(props.autoProlongableService.service);
        });
    });

    describe('если есть автоподнятие после оплаты', () => {
        let wrapper;

        beforeEach(() => {
            props.autoProlongableService = { service: 'all_sale_fresh', days: 1, base_price: 99 };
            props.autoProlongationChecked = false;
            props.onAutoBoostButtonClick = jest.fn();
            wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        });

        it('покажет картинку и кнопку включения', () => {
            expect(shallowToJson(wrapper)).toMatchSnapshot();
        });

        it('поменяет время при клике на селекте', () => {
            const select = wrapper.find(Select);
            const newTime = '15:00';
            select.simulate('change', [ newTime ]);
            const updatedSelect = wrapper.find(Select);

            expect(updatedSelect.props().value).toBe(newTime);
        });

        it('при клике на кнопку вызовет коллбэк из пропсов из передаст в него время', () => {
            const autoBoostButton = wrapper.find(Button);
            autoBoostButton.simulate('click');
            const autoBoostButtonUpdated = wrapper.find(Button);

            expect(autoBoostButtonUpdated.prop('disabled')).toBe(true);
            expect(props.onAutoBoostButtonClick).toHaveBeenCalledTimes(1);
            expect(props.onAutoBoostButtonClick).toHaveBeenCalledWith('10:00');
        });
    });
});

it('если произошла ошибка библиотеки checkout.js покажет сообщение', () => {
    props.error = { type: ERRORS.ya_checkout_fail };
    const wrapper = shallow(<BillingPaymentStatus { ...props }/>);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('если произошла ошибка инициализации платежа', () => {
    const { location } = global;
    let wrapper;

    beforeEach(() => {
        delete global.location;
        global.location = {
            reload: jest.fn(),
        };

        props.error = { type: ERRORS.init_fail };

        jest.useFakeTimers();
    });

    afterEach(() => {
        global.location = location;
    });

    it('покажет сообщение', () => {
        wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('если это ошибка про уже купленный продукт, то покажет присланный текст и не покажет кнопку', () => {
        props.error = { type: ERRORS.init_fail, error: 'PRODUCT_ALREADY_ACTIVATED', description_ru: 'i am an error' };

        wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('при нажатие на кнопку задизейблит её и перезагрузит страницу через 3 секунды', () => {
        wrapper = shallow(<BillingPaymentStatus { ...props }/>);
        const button = wrapper.find(Button);
        button.simulate('click');

        const updatedButton = wrapper.find(Button);
        expect(updatedButton.prop('disabled')).toBe(true);

        jest.advanceTimersByTime(3 * SECOND);
        expect(global.location.reload).toHaveBeenCalledTimes(1);
    });
});
