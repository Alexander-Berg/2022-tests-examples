const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardCardViewWidget = require('www-cabinet/react/components/DashboardCardViewWidget/DashboardCardViewWidget');

const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.cardView;

it('должен вернуть корретный компонент', () => {
    expect(shallowToJson(
        shallow(
            <DashboardCardViewWidget
                chartData={ [ 1, 2, 3 ] }
                compareData={{ daily: 1, weekly: 2, monthly: 3 }}
                total={ 123 }
                bunkerDict={ bunkerDict }
                hasStats={ true }
            />))).toMatchSnapshot();
});

it('должен вернуть корретный компонент, если нет данных', () => {
    expect(shallowToJson(
        shallow(
            <DashboardCardViewWidget
                bunkerDict={ bunkerDict }
                hasStats={ false }
            />))).toMatchSnapshot();
});

it('должен вернуть корретный компонент, если нет данных, но есть офферы', () => {
    expect(shallowToJson(
        shallow(
            <DashboardCardViewWidget
                hasStats={ false }
                hasOffers={ true }
                bunkerDict={ bunkerDict }
            />))).toMatchSnapshot();
});
