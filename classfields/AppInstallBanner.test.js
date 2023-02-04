const React = require('react');
const { noop } = require('lodash');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const Context = createContextProvider(contextMock);

const AppInstallBanner = require('./AppInstallBanner');

const {
    BANNER_TYPES,
} = require('auto-core/data/app-install-banners-types');

const bannerComponents = require('./bannerComponents').default;

const store = mockStore({
    cookies: {},
    config: {
        data: {
            browser: { OSFamily: 'iOS' },
            url: '/',
        },
    },
    fromWebToAppPage: {
        referer: '',
    },
    state: {
        randomChosenBanners: {},
    },
});

describe('Проверка ссылок на приложение', () => {
    for (const bannerType in BANNER_TYPES) {
        BANNER_TYPES[bannerType].forEach(currentBanner => {
            it(`${ bannerType } ${ currentBanner.name }`, async() => {
                const tree = shallow(
                    <Context>
                        <Provider store={ store }>
                            <AppInstallBanner
                                type={ bannerType }
                                onDownload={ noop }
                                onClose={ noop }
                                forcedBanner={ currentBanner }
                            />
                        </Provider>
                    </Context>,
                );

                const bannerComponent = tree.dive().dive().dive().find(bannerComponents[currentBanner.name]);

                const urlToInstall = bannerComponent.props().url;
                expect(urlToInstall).toMatchSnapshot();
            });
        });
    }
});
