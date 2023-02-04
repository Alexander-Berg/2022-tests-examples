import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

const React = require('react');
const { noop } = require('lodash');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const AppInstallBanner = require('./AppInstallBanner');
const Link = require('auto-core/react/components/islands/Link/Link');
const Context = createContextProvider(contextMock);

const OSFamilies = [ 'iOS', 'Android' ];
const testCasesRedirectSplash = [ 'touch_compare_splash' ];

OSFamilies.forEach((OSFamily) => {
    describe('Фулскрин апп-инсталл сплеш с редиректом', () => {
        const urlCode = OSFamily === 'iOS' ? 'm1nelw7' : 'eb04l75';
        const storeNewBanner = mockStore({
            config: {
                data: {
                    browser: { OSFamily },
                    pageParams: { forcedAppBannerType: 1 },
                },
            },
        });
        const storeOldBanner = mockStore({
            config: {
                data: {
                    browser: { OSFamily },
                    pageParams: { forcedAppBannerType: 0 },
                },
            },
        });

        testCasesRedirectSplash.forEach(type => {
            const redirectSplashParams = {
                actionLink: `https://app.adjust.com/${ urlCode }?campaign=${ type }&adgroup=applogo_phone_white`,
                params: {},
            };
            const testCasesView = [
                {
                    name: `(${ type } старый вид)`,
                    store: storeOldBanner,
                    component: Link,
                    adjust: { campaign: type, adgroup: 'applogo_phone_white' },
                    expectedUrl: redirectSplashParams.actionLink,
                },
                {
                    name: `(${ type } новый вид)`,
                    store: storeNewBanner,
                    component: '.FullScreenBannerWithRedirect__button_ok',
                    adjust: { campaign: type, adgroup: 'paranja' },
                    expectedUrl: `https://app.adjust.com/${ urlCode }?campaign=${ type }&adgroup=paranja`,
                },
            ];

            testCasesView.forEach((testCase) => {
                it(testCase.name, async() => {
                    const tree = shallow(
                        <Provider store={ testCase.store }>
                            <Context>
                                <AppInstallBanner
                                    OSFamily={ OSFamily }
                                    type={ type }
                                    onClose={ noop }
                                    onDownload={ noop }
                                    params={ redirectSplashParams }
                                    expFLag={ false }
                                    bannerAdjust={ testCase.adjust }
                                />
                            </Context>
                        </Provider>,
                        { context: { metrika: { sendPageEvent: noop } } },
                    );

                    const link = tree
                        .dive()
                        .dive()
                        .dive()
                        .dive()
                        .find(testCase.component);

                    const url = link.props().url;

                    expect(url).toEqual(testCase.expectedUrl);
                });
            });
        });
    });
});
