/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
const React = require('react');
const { shallow } = require('enzyme');

const BaseApp = require('./BaseApp');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const {
    ALL_BANNERS,
    APP_INSTALL_BANNERS,
} = require('auto-core/data/app-install-banners-types');

class App extends BaseApp {
    renderFooter() {
        return <footer>footer</footer>;
    }
}

function shallowRenderComponent({ props }) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <App { ...props }/>
        </ContextProvider>,
    ).dive();
}

describe('Будет выбран нужный баннер по get параметру', () => {
    const allBannerNames = Object.keys(ALL_BANNERS);
    allBannerNames.forEach(bannerName => {
        const props = {
            config: {
                isRobot: false,
                pageType: 'index',
                browser: {
                    OSFamily: 'iOS',
                },
            },
            pageController: 'index',
            dispatch: jest.fn(),
            params: {
                bannerType: bannerName,
            },
        };

        it(`для ${ bannerName } выбран тип ${ ALL_BANNERS[bannerName].campaign }`, () => {
            props.showPromoAppBanner = true;
            const page = shallowRenderComponent({ props });

            expect(page.state().appBannerType).toEqual(ALL_BANNERS[bannerName].campaign);
        });
    });
});

describe('Будет выбран нужный баннер по логике показа сплешей', () => {
    const props = {
        config: {
            isRobot: false,
            pageType: 'index',
            browser: {
                OSFamily: 'iOS',
            },
        },
        pageController: 'index',
        dispatch: jest.fn(),
        params: {},
        showPromoHeader: true,
        showPromoAppBanner: false,
        bannerID: APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH,
    };

    it(`выбран тип ${ APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH }`, () => {
        props.showPromoHeader = true;
        props.showPromoAppBanner = false;
        props.bannerID = APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH;

        const page = shallowRenderComponent({ props });

        expect(page.state().appBannerType).toEqual(APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH);
    });

    it(`выбран тип ${ APP_INSTALL_BANNERS.TOUCH_APPINSTALL_SHADE }`, () => {
        props.showPromoHeader = false;
        props.showPromoAppBanner = true;
        props.bannerID = APP_INSTALL_BANNERS.TOUCH_WELCOME_SPLASH;
        const page = shallowRenderComponent({ props });

        expect(page.state().appBannerType).toEqual(APP_INSTALL_BANNERS.TOUCH_APPINSTALL_SHADE);
    });
});
