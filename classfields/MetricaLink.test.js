const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const MetrikaLink = require('./MetrikaLink');

const props = {
    className: 'Test_Class',
    url: 'test/url/',
    title: 'Test title',
    target: '_blank',
    metrika: `mark-click,audi`,
};

it('должен отрендерить ссылку', () => {
    const context = {
        metrika: { sendPageEvent: () => '' },
    };
    const wrapper = shallow(
        <MetrikaLink { ...props }/>,
        { context: context });
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отправить метрику по клику, если метрики есть в props', () => {
    const context = {
        metrika: { sendPageEvent: jest.fn() },
    };
    const wrapper = shallow(
        <MetrikaLink { ...props }/>,
        { context: context });
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(0);
    wrapper.simulate('click');
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(1);
    expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'mark-click', 'audi' ]);
});

it('не должен отправить метрику по клику, если метрики нет в props', () => {
    const context = {
        metrika: { sendPageEvent: jest.fn() },
    };
    const noMetrikaProps = { ...props };
    noMetrikaProps.metrika = '';
    const wrapper = shallow(
        <MetrikaLink { ...noMetrikaProps }/>,
        { context: context });
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(0);
    wrapper.simulate('click');
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(0);
});

it('должен вызвать onClick из props и отправить метрику', () => {
    const context = {
        metrika: { sendPageEvent: jest.fn() },
    };
    const propsWithHandler = { ...props };
    propsWithHandler.onClick = jest.fn();
    const wrapper = shallow(
        <MetrikaLink { ...propsWithHandler }/>,
        { context: context });
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(0);
    expect(propsWithHandler.onClick.mock.calls).toHaveLength(0);
    wrapper.simulate('click', { test: test });
    expect(context.metrika.sendPageEvent.mock.calls).toHaveLength(1);
    expect(propsWithHandler.onClick.mock.calls).toHaveLength(1);
    expect(propsWithHandler.onClick).toHaveBeenCalledWith({ test: test });
});
