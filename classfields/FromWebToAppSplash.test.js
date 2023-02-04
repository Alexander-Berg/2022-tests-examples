/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const Link = require('auto-core/react/components/islands/Link/Link');
const Button = require('auto-core/react/components/islands/Button');
const FromWebToAppSplash = require('./FromWebToAppSplash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const testCases = require('./FromWebToAppSplash.testcases');

const { ADGROUPS, CAMPAIGNS } = require('./SplashTypes.json');

const OSFamilies = [ 'iOS', 'Android' ];

describe('action', () => {

    it('добавление, белый фон, логотип приложения', () => {
        const store = mockStore({
            fromWebToAppPage: {
                referer: '',
            },
            config: {
                data: {
                    pageParams: {
                        action: '',
                    },
                },
            },
        });

        const tree = shallow(
            <Provider store={ store }>
                <FromWebToAppSplash/>
            </Provider>,
            { context: contextMock },
        ).dive().dive();

        expect(tree).toHaveProp('adjust', { campaign: CAMPAIGNS.TOUCH_ADDOFFER_SPLASH, adgroup: ADGROUPS.APPLOGO_PHONE_WHITE });
    });

});

OSFamilies.forEach((OSFamily) => {
    describe(`${ OSFamily },`, () => {
        const store = mockStore({
            fromWebToAppPage: {
                referer: '',
            },
            config: {
                data: {
                    browser: { OSFamily },
                },
            },
        });
        const tree = shallow(
            <Provider store={ store }>
                <FromWebToAppSplash/>
            </Provider>,
            { context: contextMock },
        ).dive().dive().dive();

        testCases.forEach(({ expectedHref, name, props }) => {
            it(name, () => {
                const { adjust: { adgroup, campaign } } = props;

                const isButtonUsed = [
                    ADGROUPS.LOGO_PLAIN_CHOSE_ICON,
                    ADGROUPS.LOGO_3D_ANIMATED,
                    ADGROUPS.LOGO_3D_DARK,
                ].includes(adgroup);

                const SearchedComponent = isButtonUsed ? Button : Link;
                const searchedProp = 'url';

                tree.setProps(props);
                let link = tree
                    .dive()
                    .dive()
                    .find(SearchedComponent);

                switch (adgroup) {
                    case ADGROUPS.LOGO_PLAIN_CHOSE_ICON:
                        link = link.find('.FromWebToAppSplashChose__downloadButton');
                        break;
                    case ADGROUPS.LOGO_3D_ANIMATED:
                        link = link.find('.FromWebToAppSplashChoseAnimated__downloadButton');
                        break;
                }

                if (!expectedHref) {
                    const urlCode = OSFamily === 'iOS' ? 'm1nelw7' : 'eb04l75';
                    expectedHref =
                        `https://app.adjust.com/${ urlCode }?campaign=${ campaign }&adgroup=${ adgroup }`;
                }
                expect(link).toHaveProp(searchedProp, expectedHref);
            });
        });
    });
});
