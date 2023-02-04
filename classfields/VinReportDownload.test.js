const React = require('react');
const { shallow } = require('enzyme');
const { Provider } = require('react-redux');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const VinReportDownload = require('./VinReportDownload');

const Context = createContextProvider(contextMock);
const store = mockStore({});

function renderComponent() {
    return shallow(
        <Provider store={ store }>
            <Context>
                <VinReportDownload id="111111111"/>
            </Context>
        </Provider>,
    ).dive().dive().dive();
}

global.open = jest.fn(() => ({
    onbeforeunload: jest.fn(),
    onload: jest.fn(),
}));

jest.useFakeTimers();

beforeEach(() => {
    global.open.mockClear();
});

it('VinReportDownload должен отрендерить лоадер, затем через 5 сек убрать его', () => {
    const tree = renderComponent();

    expect(tree.state('isPending')).toBe(false);

    tree.find('ButtonWithLoader').simulate('click');

    // Вызвали showLoader
    expect(tree.state('isPending')).toBe(true);

    jest.runAllTimers();

    // Вызвали hideLoader
    expect(tree.state('isPending')).toBe(false);
});

it('VinReportDownload должен отправить московскую таймзону', () => {
    const tree = renderComponent();
    tree.find('ButtonWithLoader').simulate('click');

    expect(global.open.mock.calls[0][0]).toContain('&timezone=Europe%2FMoscow');
});
