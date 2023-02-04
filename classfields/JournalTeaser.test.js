/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');

const { mount, shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const mockMetrika = require('autoru-frontend/mocks/metrikaMock');
const mockJournal = require('autoru-frontend/mockData/state/journal.mock');

const JournalTeaser = require('./JournalTeaser');

const store = mockStore({
    journalWidget: mockJournal,
});

it('Должен отрендерить JournalTeaser (список статей)', () => {
    const Context = createContextProvider(contextMock);

    const teaser = mount(
        <Context>
            <Provider store={ store }>
                <JournalTeaser/>
            </Provider>
        </Context >,
    );
    expect(shallowToJson(teaser)).toMatchSnapshot();
});

it('Не падает, если нет данных journalWidget в сторе', () => {
    const teaser = shallow(
        <JournalTeaser/>,
        { context: { store: mockStore(), hasExperiment: () => false, metrika: mockMetrika } },
    ).dive();
    expect(teaser.isEmptyRender()).toBe(true);
});
