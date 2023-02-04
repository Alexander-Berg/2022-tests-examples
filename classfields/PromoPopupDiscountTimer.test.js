const React = require('react');

const PromoPopupDiscountTimer = require('./PromoPopupDiscountTimer');
const Timer = require('auto-core/react/components/common/Timer/Timer').default;
const MockDate = require('mockdate');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

let page;

jest.useFakeTimers();

const props = {
    data: {
        discountExpiresDate: '2019-05-22 19:00:00', // это время по MSK
        discountValue: '70',
    },
    onClick: jest.fn(),
    onTimerFinish: jest.fn(),
    isOpened: false,
};

beforeEach(() => {
    // допустим мы в магадане
    MockDate.set('2019-05-22T13:00:00.000+1100');
    page = shallow(<PromoPopupDiscountTimer { ...props }/>);
});

afterEach(() => {
    MockDate.reset();
});

it('правильно рисуется', () => {
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('ставит время таймера по москве', () => {
    const timer = page.find(Timer);
    expect(timer.prop('expiresDate')).toBe('2019-05-22T19:00:00+03:00');
});

it('при клике на себя вызывает коллбэк', () => {
    page.simulate('click');
    expect(props.onClick).toHaveBeenCalledTimes(1);
});

it('по окончании таймера вызывает коллбэк', () => {
    const timer = page.find(Timer);
    timer.simulate('timerFinish');
    expect(props.onTimerFinish).toHaveBeenCalledTimes(1);
});

it('скрывает панель когда надо', () => {
    page.setProps({ isOpened: true });
    expect(page.hasClass('PromoPopupDiscountTimer_hidden')).toBe(true);
});
