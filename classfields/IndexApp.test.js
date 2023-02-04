/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const configMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const Context = createContextProvider(contextMock);

const { APP_INSTALL_BANNERS } = require('auto-core/data/app-install-banners-types');

const IndexApp = require('./IndexApp');

const initialState = {
    ads: {},
    appPromo: {
        showPromoHeader: false,
    },
    config: configMock
        .withPageType('index')
        .withBrowser({
            OSFamily: 'iOS',
            OSVersion: '13.2.3',
        })
        .value(),
    router: {
        current: {
            data: {
                controller: 'index',
            },
        },
    },
};

it('должен отрисовать апп главной без сплэша', () => {
    const store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <Context>
                <IndexApp/>
            </Context>
        </Provider>,
    ).dive().dive();

    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper.find('Connect(HomescreenModal)')).not.toExist();
    expect(wrapper.find('Connect(PromoHeader)')).not.toExist();
});

it('должен отрисовать апп главной и сплеш', () => {
    const store = mockStore({
        ...initialState,
        appPromo: {
            showPromoHeader: true,
            showPromoAppBanner: false,
            bannerID: APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH,
        },
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <Context>
                <IndexApp/>
            </Context>
        </Provider>,
    ).dive().dive().dive();

    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper.find('Connect(HomescreenModal)')).not.toExist();
    expect(wrapper.find('Connect(AppInstallBanner)')).toExist();
});

it('должен отрисовать апп главной и модал', () => {
    const store = mockStore({
        ...initialState,
        appPromo: {
            showPromoHeader: true,
        },
        config: configMock
            .withPageType('index')
            .withBrowser({
                OSFamily: 'iOS',
                OSVersion: '10.2.3',
            })
            .value(),
    });

    const wrapper = shallow(
        <Provider store={ store }>
            <Context>
                <IndexApp/>
            </Context>
        </Provider>,
    ).dive().dive().dive();

    expect(wrapper).not.toBeEmptyRender();
    expect(wrapper.find('Connect(HomescreenModal)')).toExist();
    expect(wrapper.find('Connect(PromoHeader)')).not.toExist();
});
