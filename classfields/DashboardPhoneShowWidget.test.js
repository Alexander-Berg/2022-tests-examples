const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardPhoneShowWidget = require('www-cabinet/react/components/DashboardPhoneShowWidget/DashboardPhoneShowWidget');

const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.phoneShow;
const chartData = require('./mocks/chartData');
const compareData = require('./mocks/compareData');

it('должен вернуть корректный компонент', () => {
    expect(shallowToJson(
        shallow(
            <DashboardPhoneShowWidget
                hasStats={ true }
                bunkerDict={ bunkerDict }
                chartData={ chartData }
                compareData={ compareData }
                total={ 123 }
                conversion={ 0 }
            />))).toMatchSnapshot();
});

it('должен вернуть корретный компонент, если !hasStats', () => {
    expect(shallowToJson(
        shallow(
            <DashboardPhoneShowWidget
                hasStats={ false }
                bunkerDict={ bunkerDict }
            />))).toMatchSnapshot();
});

it('должен вернуть корретный компонент, если hasOffers и !hasStats', () => {
    expect(shallowToJson(
        shallow(
            <DashboardPhoneShowWidget
                hasStats={ false }
                bunkerDict={ bunkerDict }
                hasOffers={ true }
            />))).toMatchSnapshot();
});
