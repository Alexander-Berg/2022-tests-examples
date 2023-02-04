const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardWalkInWidget = require('www-cabinet/react/components/DashboardWalkInWidget');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const chartData = require('./mocks/chartData');
const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.walkIn;

it('должен вернуть корректный компонент DashboardWidget', () => {
    const tree = shallow(
        <ContextProvider>
            <DashboardWalkInWidget
                bunkerDict={ bunkerDict }
                chartData={ chartData }
                total={ 123 }
                visitsData={{ unconfirmed_visits: 100, confirmed_visits: 23 }}
                routeParams={{ from: '2011-01-02', to: '2012-01-02' }}
            />
        </ContextProvider>,
    ).dive();

    const widget = tree.find('DashboardWidget');

    expect(shallowToJson(widget)).toMatchSnapshot();
});
