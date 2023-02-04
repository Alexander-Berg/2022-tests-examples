/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const _ = require('lodash');
const React = require('react');
const BillingFrameTiedCards = require('./BillingFrameTiedCards');
const Button = require('auto-core/react/components/islands/Button');
const Select = require('auto-core/react/components/islands/Select');
const Item = require('auto-core/react/components/islands/Item');
const Link = require('auto-core/react/components/islands/Link');
const { formatTiedCardInfo, methods } = require('autoru-frontend/mockData/responses/billing/paymentMethods.mock');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const DEFAULT_PROPS = {
    cards: [
        formatTiedCardInfo(methods.oldApiTiedCard),
        formatTiedCardInfo(methods.newApiTiedCard),
        formatTiedCardInfo(methods.newApiTiedCardWithoutBrand),
    ],
    price: 90,
    onPayButtonClick: () => {},
    onAddCardButtonClick: () => {},
    onTokenizeError: () => {},
};

const YooMoneyCheckoutTokenizeCsc = jest.fn();
const YooMoneyCheckout = jest.fn().mockImplementation(() => ({
    tokenizeCsc: YooMoneyCheckoutTokenizeCsc,
}));

beforeEach(() => {
    YooMoneyCheckout.mockClear();
    YooMoneyCheckoutTokenizeCsc.mockClear();

    global.YooMoneyCheckout = YooMoneyCheckout;
});

afterEach(() => {
    global.YooMoneyCheckout = undefined;
});

describe('компонент <BillingFrameTiedCards/> для десктопа', () => {
    let wrapper;
    let props;

    beforeEach(() => {
        props = _.cloneDeep(DEFAULT_PROPS);
        props.onAddCardButtonClick = jest.fn();
        wrapper = shallowRenderComponent(props);
    });

    it('правильно рисуется', () => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('позволяет поменять карту для оплаты', () => {
        const select = wrapper.find(Select);
        const newValue = props.cards[1].mask;
        select.simulate('change', [ newValue ]);
        const updatedSelect = wrapper.find(Select);
        expect(updatedSelect.props().value).toBe(newValue);
    });

    it('добавить в конец ссылку на добавление новой карты', () => {
        const items = wrapper.find(Item);
        expect(items).toHaveLength(props.cards.length + 1);
        const addNewCardLink = items.last().find(Link);
        expect(addNewCardLink.dive().text()).toBe('Добавить карту');
    });

    it('при клике на ссылку "Добавить карту" вызовет коллбэк', () => {
        wrapper.find('Select').simulate('change', 'new');
        expect(props.onAddCardButtonClick).toHaveBeenCalledTimes(1);
    });
});

it('если есть только одна привязанная карта, то не покажет чекбокс основной карты', () => {
    const props = _.cloneDeep(DEFAULT_PROPS);
    props.cards = props.cards.slice(0, 1);
    const wrapper = shallowRenderComponent(props);

    expect(wrapper.find('.BillingFrameTiedCards__checkbox')).toHaveLength(0);
});

it('при клике на кнопку передаст в коллбэк выбранную карту', () => {
    const props = _.cloneDeep(DEFAULT_PROPS);
    props.onPayButtonClick = jest.fn();
    const wrapper = shallowRenderComponent(props);
    const submitButton = wrapper.find(Button);

    submitButton.simulate('click');

    expect(props.onPayButtonClick).toHaveBeenCalledTimes(1);
    expect(props.onPayButtonClick).toHaveBeenCalledWith(props.cards[0]);
});

it('при клике на чекбокс "всегда оплачивать с этой карты" вызовет коллбэк из пропсов', () => {
    const props = _.cloneDeep(DEFAULT_PROPS);
    props.updateCardInfo = jest.fn();

    const wrapper = shallowRenderComponent(props);
    const checkbox = wrapper.find('.BillingFrameTiedCards__checkbox');

    checkbox.simulate('check');

    expect(props.updateCardInfo).toHaveBeenCalledTimes(1);
    expect(props.updateCardInfo).toHaveBeenCalledWith({ card_id: '444444|4448', payment_system_id: 'yandexkassa', preferred: true });
});

it('при апдейте списка карт обновит внутренный state карты', () => {
    const props = _.cloneDeep(DEFAULT_PROPS);
    props.cards[0].prefered = false;
    const wrapper = shallowRenderComponent(props);

    const newProps = _.cloneDeep(DEFAULT_PROPS);
    const updatedCard = newProps.cards[0];
    // допустим мы поставили флаг prefered
    updatedCard.prefered = true;
    wrapper.setProps(newProps);
    wrapper.update();

    const select = wrapper.find(Select);
    const checkbox = wrapper.find('.BillingFrameTiedCards__checkbox');

    expect(select.prop('value')).toBe(updatedCard.mask);
    expect(checkbox.prop('checked')).toBe(updatedCard.preferred);
});

it('при апдейте списка карт установит первую карту как выбранную, если текущую не нашли', () => {
    const props = _.cloneDeep(DEFAULT_PROPS);
    const wrapper = shallowRenderComponent(props, false);

    const newProps = _.cloneDeep(DEFAULT_PROPS);
    const newCard = formatTiedCardInfo(methods.newApiTiedCard2);
    // заменяем первую (выбраннуую) карту на другую
    newProps.cards = [
        newCard,
        props.cards[1],
    ];
    wrapper.setProps(newProps);
    wrapper.update();

    const select = wrapper.find(Select);
    const checkbox = wrapper.find('.BillingFrameTiedCards__checkbox');

    expect(select.prop('value')).toBe(newCard.mask);
    expect(checkbox.prop('checked')).toBe(newCard.preferred);
});

describe('для карты с цвц-подтверждением', () => {
    let props;

    beforeEach(() => {
        props = _.cloneDeep(DEFAULT_PROPS);
        props.cards[0].verification_required = true;
        props.onPayButtonClick = jest.fn();
        props.onTokenizeError = jest.fn();
    });

    it('нарисует поле для ввода цвц-кода', () => {
        const page = shallowRenderComponent(props);
        const cvcBlock = page.find('.BillingFrameTiedCards__cvc');

        expect(shallowToJson(cvcBlock)).toMatchSnapshot();
    });

    it('при попытке оплатить покажет ошибку если введено меньше 3 цифр', () => {
        const page = shallowRenderComponent(props);
        simulateCvcCardPay(page, { cvc: 99 });

        const cvcErrorText = page.find('.BillingFrameTiedCards__cvcText');
        const updatedCvcInput = page.find('.BillingFrameTiedCards__cvcInput');

        expect(cvcErrorText).toHaveLength(0);
        expect(updatedCvcInput.prop('error')).toBe('Введите корректный CVC-код');
    });

    describe('при оплате', () => {
        const CODE = 999;
        const TOKEN = 'i am token';
        const successfulResponsePromise = Promise.resolve({ data: { response: { cscToken: TOKEN } } });
        const noTokenPromise = Promise.resolve({ data: { response: { } } });
        const failedRequestPromise = Promise.reject();

        it('инициализирует инстанс YooMoneyCheckout', () => {
            YooMoneyCheckoutTokenizeCsc.mockImplementationOnce(() => successfulResponsePromise);
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            expect(YooMoneyCheckout).toHaveBeenCalledTimes(1);
        });

        it('запросит токенизированный цвц', () => {
            YooMoneyCheckoutTokenizeCsc.mockImplementationOnce(() => successfulResponsePromise);
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            expect(YooMoneyCheckoutTokenizeCsc).toHaveBeenCalledTimes(1);
            expect(YooMoneyCheckoutTokenizeCsc).toHaveBeenCalledWith({ csc: String(CODE) });
        });

        it('если все успешно передаст токен в коллбэк', () => {
            YooMoneyCheckoutTokenizeCsc.mockImplementationOnce(() => successfulResponsePromise);
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            return successfulResponsePromise
                .then(() => {
                    expect(props.onPayButtonClick).toHaveBeenCalledTimes(1);
                    expect(props.onPayButtonClick.mock.calls[0][0].cvcToken).toBe(TOKEN);
                });
        });

        it('если в ответе нет токена вызовет коллбэк с ошибкой', () => {
            YooMoneyCheckoutTokenizeCsc.mockImplementationOnce(() => noTokenPromise);
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            return noTokenPromise
                .then(() => new Promise((resolve) => process.nextTick(resolve)))
                .then(() => {
                    expect(props.onTokenizeError).toHaveBeenCalledTimes(1);
                });
        });

        it('если нет библиотеки checkout.js вызовет коллбэк с ошибкой', () => {
            global.YooMoneyCheckout = undefined;
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            expect(props.onTokenizeError).toHaveBeenCalledTimes(1);
        });

        it('если произошла ошибка при токенизации вызовет коллбэк с ошибкой', () => {
            YooMoneyCheckoutTokenizeCsc.mockImplementationOnce(() => failedRequestPromise);
            const page = shallowRenderComponent(props);
            simulateCvcCardPay(page, { cvc: CODE });

            return failedRequestPromise.then().then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                () => {
                    expect(props.onTokenizeError).toHaveBeenCalledTimes(1);
                },
            );
        });
    });
});

function shallowRenderComponent(props, disableLifecycleMethods = true) {
    const page = shallow(<BillingFrameTiedCards { ...props }/>, { disableLifecycleMethods });
    const instance = page.instance();
    instance.cvcInput = {
        focus: () => { },
    };
    instance.componentDidMount();

    return page;
}

function simulateCvcCardPay(page, { cvc }) {
    const cvcInput = page.find('.BillingFrameTiedCards__cvcInput');
    cvcInput.simulate('change', cvc);

    const submitButton = page.find(Button);
    submitButton.simulate('click');
}
