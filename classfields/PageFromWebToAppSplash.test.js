/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const PageFromWebToAppSplash = require('./PageFromWebToAppSplash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const testCases = require('./PageFromWebToAppSplash.testcases');

const OSFamilies = [ 'iOS', 'Android' ];

OSFamilies.forEach((OSFamily) => {
    describe(`${ OSFamily },`, () => {
        const store = mockStore({
            config: {
                data: {
                    browser: { OSFamily },
                    pageType: 'compare',
                },
            },
        });

        testCases.forEach(({ expectedHref, name, props }) => {
            it(name, () => {
                const tree = shallow(
                    <Provider store={ store }>
                        <PageFromWebToAppSplash { ...props }/>
                    </Provider>,
                    { context: contextMock },
                ).dive().dive().dive();

                expect(tree.instance().getUrl()).toEqual(expectedHref[OSFamily]);
            });
        });
    });
});
