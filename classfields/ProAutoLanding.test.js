/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const myReportsMock = require('auto-core/react/dataDomain/myReports/mocks/myReports.mock');

const ProAutoLanding = require('./ProAutoLanding');

let state;
let context;

global.scroll = jest.fn();

const props = {
    onSubmit: jest.fn(),
    isAuth: true,
    myReports: myReportsMock,
    noIframe: true,
    notify: {
        show: false,
        header: 'string',
        text: 'string',
    },
    reportsBundles: [],
};

beforeEach(() => {
    state = {
        bunker: {},
        config: configStateMock.value(),
        card: {},
        user: { data: { } },
        state: { authModal: {} },
        carCheck: {},
    };

    context = _.cloneDeep(contextMock);
});

describe('при заходе на страницу с action=scroll-to-reports', () => {
    const originalWindowLocation = global.location;
    const originalWindowHistoryReplaceState = global.history.replaceState;

    beforeEach(() => {
        global.location = { href: 'https://auto.ru/history/?action=scroll-to-reports' };
        global.history.replaceState = jest.fn();
    });

    afterEach(() => {
        global.location = originalWindowLocation;
        global.history.replaceState = originalWindowHistoryReplaceState;
    });

    it('должен удалить action из урла', () => {
        shallow(
            <ProAutoLanding { ...props }/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive();

        expect(global.history.replaceState).toHaveBeenCalledWith(global.window.history.state, '', 'https://auto.ru/history/');
    });

    it('должен подскроллить к отчётам, если они есть', () => {
        React.createRef = () => ({ current: { scrollIntoView: jest.fn() } });

        const instance = shallow(
            <ProAutoLanding { ...props }/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive().instance();

        expect(instance.myReportsRef.current.scrollIntoView).toHaveBeenCalled();
        expect(instance.topRef.current.scrollIntoView).not.toHaveBeenCalled();
    });

    it('должен подскроллить к блоку со строкой поиска, если отчётов есть', () => {
        React.createRef = () => ({ current: { scrollIntoView: jest.fn() } });

        const instance = shallow(
            <ProAutoLanding { ...props } myReports={{ ...myReportsMock, reports: [] }}/>,
            { context: { ...context, store: mockStore(state) } },
        ).dive().instance();

        expect(instance.myReportsRef.current.scrollIntoView).not.toHaveBeenCalled();
        expect(instance.topRef.current.scrollIntoView).toHaveBeenCalled();
    });
});
