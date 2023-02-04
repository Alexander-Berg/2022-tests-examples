const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardWalletWidget = require('www-cabinet/react/components/DashboardWalletWidget');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const chartData = require('./mocks/chartData');
const legend = require('./mocks/legend');
const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.wallet;

it('должен вернуть DashboardWalletWidget', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardWalletWidget
                bunkerDict={ bunkerDict }
                chartData={ chartData }
                total={ 123 }
                hasStats={ true }
                hasOffers={ true }
                legend={ legend }
                routeParams={{ from: '2011-01-02', to: '2012-01-02' }}
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен вернуть DashboardWalletWidget, если !hasOffers', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardWalletWidget
                bunkerDict={ bunkerDict }
                hasStats={ false }
                routeParams={{ from: '2011-01-02', to: '2012-01-02' }}
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});

it('должен вернуть DashboardWalletWidget, если hasOffers && !hasStats', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardWalletWidget
                bunkerDict={ bunkerDict }
                hasStats={ false }
                hasOffers={ true }
                routeParams={{ from: '2011-01-02', to: '2012-01-02' }}
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});
