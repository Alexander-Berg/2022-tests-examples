const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userMock = require('auto-core/react/dataDomain/user/mocks').default;

const ServiceNavigation = require('./ServiceNavigation');

it('должен отрендерить компонент ServiceNavigation', () => {
    const store = mockStore({
        config: { data: { pageType: 'sales' } },
        user: { data: { id: '123' } },
    });
    const context = {
        link: (routeName) => `${ routeName }`,
        linkOld: (url, params, domain) => `${ domain }-${ url }`,
        store,
    };
    const wrapper = shallow(<ServiceNavigation/>, { context: context });
    expect(shallowToJson(wrapper.dive().dive())).toMatchSnapshot();
});

it('должен вернуть корректную ссылку на объявления для премиум перекупа', () => {
    const store = mockStore({
        config: { data: { pageType: 'sales' } },
        user: userMock.withAuth(true).withPremiumReseller(true).value(),
    });
    const context = {
        link: (routeName) => `${ routeName }`,
        linkOld: (url, params, domain) => `${ domain }-${ url }`,
        store,
    };
    const wrapper = shallow(<ServiceNavigation/>, { context: context });
    const navigationItems = wrapper.dive().dive().find('ServiceNavigation').props().navigationItems;
    const salesItem = navigationItems.find(item => item.id === 'sales');
    expect(salesItem.url).toBe('reseller-sales');
});
