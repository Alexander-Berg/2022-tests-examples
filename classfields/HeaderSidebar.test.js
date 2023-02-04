const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMockBrowser');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const ContextProvider = createContextProvider(contextMock);
const HeaderSidebar = require('./HeaderSidebar');

const props = {
    route: {
        data: {
            controller: 'listing',
        },
    },
    pageParams: {
        mark: 'bmw',
    },
    sidebarId: 'sidebar',
};

describe('HeaderSidebar', () => {
    it('пункт "Мои объявления" должен вести на мобильную версию /my-main/', async() => {
        const { pageParams, route, sidebar } = props;

        const wrapper = shallow(
            <ContextProvider>
                <HeaderSidebar pageParams={ pageParams } route={ route } sidebarId={ sidebar }/>
            </ContextProvider>,
        ).dive();

        expect(
            wrapper.find('.HeaderSidebar__link').at(1),
        ).toHaveProp('url', 'link/my-main/?');
    });

    it('пункт "Полная версия" должен вести на главную десктопной версии', async() => {
        const { pageParams, route, sidebar } = props;

        const wrapper = shallow(
            <ContextProvider>
                <HeaderSidebar pageParams={ pageParams } route={ route } sidebarId={ sidebar }/>
            </ContextProvider>,
        ).dive();

        expect(
            wrapper.find('.HeaderSidebar__footer-link').at(5),
        ).toHaveProp('url', 'linkDesktop/index/?nomobile=true');
    });

    it('пункт "Журнал" должен вести на mag.auto', async() => {
        const { pageParams, route, sidebar } = props;

        const wrapper = shallow(
            <ContextProvider>
                <HeaderSidebar pageParams={ pageParams } route={ route } sidebarId={ sidebar }/>
            </ContextProvider>,
        ).dive();

        expect(
            wrapper.find('.HeaderSidebar__footer-link').at(8),
        ).toHaveProp('url', 'linkMag/mag-index/?');
    });
});
