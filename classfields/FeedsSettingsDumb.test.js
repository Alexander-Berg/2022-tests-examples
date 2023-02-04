/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { mount, shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { Provider } = require('react-redux');
const _ = require('lodash');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const routertMock = require('autoru-frontend/mocks/routerMock');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const feedsMock = require('www-cabinet/react/dataDomain/feeds/mocks/feeds.mock');
const tariffsMock = require('www-cabinet/react/dataDomain/promoPopup/mocks/tariffs');

const getAvailableManualCategories = require('www-cabinet/react/dataDomain/feeds/selectors/getAvailableManualCategories');
const getFeeds = require('www-cabinet/react/dataDomain/feeds/selectors/getFeeds');
const getCategoriesNames = require('www-cabinet/react/dataDomain/feeds/selectors/getCategoriesNames');
const getNode = require('auto-core/react/dataDomain/bunker/selectors/getNode');

const FeedsSettingsDumb = require('./FeedsSettingsDumb');

const initialState = {
    bunker: getBunkerMock([ 'cabinet/feeds', 'cabinet/categories_info' ]),
    feeds: feedsMock,
    promoPopup: { tariffs: tariffsMock },
    user: { data: {} },
};

const store = mockStore(initialState);
const ContextProvider = createContextProvider({ ...contextMock, router: routertMock });

const baseProps = {
    canWrite: true,
    categoriesNames: _.pick(getCategoriesNames(initialState), [ 'LCV_USED' ]),
    feeds: getFeeds(initialState),
    feedsDict: _.pick(getNode(initialState, 'cabinet/feeds'), [ 'emptySettingsPlaceholder' ]),
    availableAutomaticCategories: [],
    availableManualCategories: getAvailableManualCategories(initialState).slice(0, 1),
    isRegionWithForbiddenAvitoFeed: false,

    saveFeed: _.noop,
    loadFeed: _.noop,
};

describe('должен вызывать события метрики', () => {
    const formValues = {
        type: 'type',
        hash: 'hash',
        deleteSale: 'deleteSale',
        leaveServices: 'leaveServices',
        leaveAddedImages: 'leaveAddedImages',
    };

    const CASES = [
        { type: 'add', title: 'при сабмите добавления фида', action: 'sendAddFormSubmitMetrics' },
        { type: 'edit', title: 'при сабмите добавления фида', action: 'sendEditFormSubmitMetrics' },
    ];

    const handler = contextMock.metrika.sendPageEvent;

    afterEach(() => {
        handler.mockClear();
    });

    CASES.forEach((testCase) => {
        it(testCase.title, () => {
            const tree = mount(
                <ContextProvider>
                    <Provider store={ store }>
                        <FeedsSettingsDumb
                            { ...baseProps }
                            feedsDict={ getNode(initialState, 'cabinet/feeds') }
                        />
                    </Provider>
                </ContextProvider>,
            );

            const instance = tree.find('FeedsSettings').instance();

            instance[testCase.action](formValues);

            expect(handler).toHaveBeenNthCalledWith(1, [ testCase.type, 'submit', `type-${ formValues.type }` ]);
            expect(handler).toHaveBeenNthCalledWith(2, [ testCase.type, 'submit', `category-${ formValues.hash }` ]);
            expect(handler).toHaveBeenNthCalledWith(3, [ testCase.type, 'submit', `delete_sale-${ formValues.deleteSale }` ]);
            expect(handler).toHaveBeenNthCalledWith(4, [ testCase.type, 'submit', `leave_services-${ formValues.leaveServices }` ]);
            expect(handler).toHaveBeenNthCalledWith(5, [ testCase.type, 'submit', `leave_added_images-${ formValues.leaveAddedImages }` ]);
        });
    });
});

it('должен отрендерить форму добавления и форму редактирования фида', () => {
    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <FeedsSettingsDumb
                    { ...baseProps }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive();

    const container = tree.find('.FeedsSettings__container');

    expect(shallowToJson(container)).toMatchSnapshot();
});

it('не должен рендерить форму добавления, если нет прав', () => {
    const tree = mount(
        <ContextProvider>
            <Provider store={ store }>
                <FeedsSettingsDumb
                    { ...baseProps }
                    canWrite={ false }
                    feedsDict={ getNode(initialState, 'cabinet/feeds') }
                />
            </Provider>
        </ContextProvider>,
    );

    const container = tree.find('FeedsSettingsAddForm');
    expect(shallowToJson(container)).toBeNull();
});

it('должен рендерить плейсхолдер, если нет фидов и нет доступных категорий', () => {
    const ContextProvider = createContextProvider({ ...contextMock, router: routertMock });

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <FeedsSettingsDumb
                    { ...baseProps }
                    feeds={ [] }
                    availableAutomaticCategories={ [] }
                    availableManualCategories={ [] }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive();

    const container = tree.find('.FeedsSettings__placeholder');

    expect(shallowToJson(container)).toMatchSnapshot();
});
