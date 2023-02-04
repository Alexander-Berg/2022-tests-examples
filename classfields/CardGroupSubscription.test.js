const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const Button = require('auto-core/react/components/islands/Button');

const CardGroupSubscriptionDumb = require('./CardGroupSubscriptionDumb');

const CARD_GROUP_SUBSCRIPTION_POPUP_PROPS = {
    mainOffset: 20,
    secondaryOffset: -17,
    directions: [ 'bottom-left' ],
    textSubscribe: 'Подписаться',
    textSubscribed: 'Вы подписаны',
    showIcon: true,
};

it('должен корректно отрендерить кнопку подписки на группе новых', () => {
    const tree = shallow(
        <CardGroupSubscriptionDumb
            className="SomeClassName"
            { ...CARD_GROUP_SUBSCRIPTION_POPUP_PROPS }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен содержать переданный класс', () => {
    const tree = shallow(
        <CardGroupSubscriptionDumb
            className="SomeClassName"
            { ...CARD_GROUP_SUBSCRIPTION_POPUP_PROPS }
        />,
    );
    expect(tree.find('.SomeClassName')).toHaveLength(1);
});

it('должен содержать текст "Подписаться", если не подписан', () => {
    const tree = shallow(
        <CardGroupSubscriptionDumb
            className="SomeClassName"
            { ...CARD_GROUP_SUBSCRIPTION_POPUP_PROPS }
        />,
    );
    expect(tree.find('Button').childAt(1).text()).toContain('Подписаться');
});

it('должен содержать текст "Вы подписаны", если подписан (если передан объект "subscription")', () => {
    const tree = shallow(
        <CardGroupSubscriptionDumb
            className="SomeClassName"
            { ...CARD_GROUP_SUBSCRIPTION_POPUP_PROPS }
            subscription={{ data: {} }}
        />,
    );
    expect(tree.find('Button').childAt(1).text()).toContain('Вы подписаны');
});

it('должен поменять цвет кнопки если нет текста и подписан (если передан объект "subscription")', () => {
    const tree = shallow(
        <CardGroupSubscriptionDumb
            className="SomeClassName"
            { ...CARD_GROUP_SUBSCRIPTION_POPUP_PROPS }
            textSubscribe={ null }
            textSubscribed={ null }
            subscription={{ data: {} }}
        />,
    );
    expect(tree.find('Button').prop('color')).toBe(Button.COLOR.TRANSPARENT_BLACK_BLUE);
});
