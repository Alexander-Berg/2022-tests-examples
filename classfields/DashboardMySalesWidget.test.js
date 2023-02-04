const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardMySalesWidget = require('www-cabinet/react/components/DashboardMySalesWidget/DashboardMySalesWidget');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const chartData = require('./mocks/chartData');
const clientUrls = require('./mocks/clientUrls');
const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.mySales;

it('должен вернуть DashboardMySalesWidget', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardMySalesWidget
                bunkerDict={ bunkerDict }
                chartData={ chartData }
                total={ 123 }
                hasOffers={ true }
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен вернуть DashboardMySalesWidget, если нет офферов', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardMySalesWidget
                bunkerDict={ bunkerDict }
                hasOffers={ false }
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен вернуть DashboardMySalesWidget, если нет офферов и есть clientsUrl', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardMySalesWidget
                bunkerDict={ bunkerDict }
                hasOffers={ false }
                clientUrls={ clientUrls }
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен вернуть DashboardMySalesWidget, если нет офферов, canAddOffers и clientsUrl', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardMySalesWidget
                bunkerDict={ bunkerDict }
                hasOffers={ false }
                canAddOffers={ true }
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});
