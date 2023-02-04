const _ = require('lodash');
const React = require('react');
const BillingFrameWallet = require('./BillingFrameWallet');
const Button = require('auto-core/react/components/islands/Button');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const DEFAULT_PROPS = {
    price: 90,
    balance: 120,
    onPayButtonClick: () => {},
    isMobile: false,
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисует компонент', () => {
    props.isMobile = false;
    const wrapper = shallow(<BillingFrameWallet { ...props }/>);

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('при клике на кнопку "Оплатить" вызовет коллбэк', () => {
    props.onPayButtonClick = jest.fn();
    const wrapper = shallow(<BillingFrameWallet { ...props }/>);
    const payButton = wrapper.find(Button);

    payButton.simulate('click');

    expect(props.onPayButtonClick).toHaveBeenCalledTimes(1);
});
