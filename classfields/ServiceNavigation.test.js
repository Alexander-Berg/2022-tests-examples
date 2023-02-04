const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ServiceNavigation = require('./ServiceNavigation');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

const navigationItems =
[ { name: 'sales',
    title: 'Объявления',
    url: 'sales',
    metrika: 'tabs,my',
    active: true },
{ name: 'reviews',
    title: 'Отзывы',
    url: 'my-reviews',
    metrika: 'tabs,my,reviews',
    active: false },
{ name: 'messages',
    title: 'Сообщения',
    url: 'forum-/messages/',
    metrika: 'tabs,messages' },
];

it('должен отрендерить компонент ServiceNavigation', () => {
    const wrapper = shallow(
        <Provider store={ store }>
            <ServiceNavigation navigationItems={ navigationItems }/>
        </Provider>,
    ).dive().dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно рисовать активную ссылку в меню', () => {
    const wrapper = shallow(
        <Provider store={ store }>
            <ServiceNavigation navigationItems={ navigationItems }/>
        </Provider>,
    ).dive().dive();
    const salesMenuLink = wrapper.find('div[className="ServiceNavigation"]>.ServiceNavigation__link[children="Объявления"]');
    expect(salesMenuLink.props().className).toEqual(expect.stringContaining('ServiceNavigation__link_active'));
    const otherMenuLink = wrapper.find('div[className="ServiceNavigation"]>.ServiceNavigation__link[children="Отзывы"]');
    expect(otherMenuLink.props().className).toEqual(expect.not.stringContaining('ServiceNavigation__link_active'));
});

it('не должен падать, если нет активной ссылки', () => {
    const wrapper = shallow(
        <Provider store={ store }>
            <ServiceNavigation navigationItems={ navigationItems }/>
        </Provider>,
    ).dive().dive();
    expect(wrapper.isEmptyRender()).toBe(false);
});
