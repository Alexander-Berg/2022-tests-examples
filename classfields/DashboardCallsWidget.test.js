const React = require('react');
const { shallow } = require('enzyme');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const DashboardCallsWidget = require('www-cabinet/react/components/DashboardCallsWidget/DashboardCallsWidget');

const bunkerDict = getBunkerMock([ 'cabinet/dashboard' ], { spreadSubNodes: true }).widgets.calls;
const emptyChartData = require('./mocks/emptyChartData');

const contextObject = {
    context: { linkCabinet: jest.fn() },
};

it('должен вернуть DashboardCallsWidget', () => {
    const DashboardCallsWidgetInstance = shallow(
        <DashboardCallsWidget
            hasOffers={ true }
            hasStats={ true }
            hasCallTracking={ true }
            bunkerDict={ bunkerDict }
            chartData={ emptyChartData }
            total={ 20 }
            conversion={ 0 }
        />, contextObject).instance();
    DashboardCallsWidgetInstance.renderCharts = () => 'DashboardCallsWidgetCharts';

    expect(DashboardCallsWidgetInstance.render()).toMatchSnapshot();
});

describe('renderPlaceholder', () => {
    it('должен вызвать renderNoCallTrackingPlaceholder, если !hasCallTracking', () => {
        const DashboardCallsWidgetInstance = shallow(
            <DashboardCallsWidget
                hasStats={ true }
                hasOffers={ true }
                hasCallTracking={ false }
                bunkerDict={ bunkerDict }
                canWriteCallTracking={ true }
            />, contextObject).instance();

        DashboardCallsWidgetInstance.renderNoCallTrackingPlaceholder = jest.fn();
        DashboardCallsWidgetInstance.renderPlaceholder();
        expect(DashboardCallsWidgetInstance.renderNoCallTrackingPlaceholder).toHaveBeenCalled();
    });

    it('должен вызвать renderNoOffersPlaceholder, !hasOffers', () => {
        const DashboardCallsWidgetInstance = shallow(
            <DashboardCallsWidget
                hasStats={ false }
                hasOffers={ false }
                hasCallTracking={ true }
                bunkerDict={ bunkerDict }
            />, contextObject).instance();

        DashboardCallsWidgetInstance.renderNoOffersPlaceholder = jest.fn();
        DashboardCallsWidgetInstance.renderPlaceholder();
        expect(DashboardCallsWidgetInstance.renderNoOffersPlaceholder).toHaveBeenCalled();
    });

    it('должен вызвать renderNoStatsPlaceholder', () => {
        const DashboardCallsWidgetInstance = shallow(
            <DashboardCallsWidget
                hasStats={ false }
                hasOffers={ true }
                hasCallTracking={ true }
                bunkerDict={ bunkerDict }
            />, contextObject).instance();

        DashboardCallsWidgetInstance.renderNoStatsPlaceholder = jest.fn();
        DashboardCallsWidgetInstance.renderPlaceholder();
        expect(DashboardCallsWidgetInstance.renderNoStatsPlaceholder).toHaveBeenCalled();
    });
});

it('renderCharts: должен вернуть графики звонков', () => {
    const DashboardCallsWidgetInstance = shallow(
        <DashboardCallsWidget
            hasOffers={ true }
            hasStats={ true }
            hasCallTracking={ true }
            bunkerDict={ bunkerDict }
            chartData={ emptyChartData }
            total={ 20 }
        />, contextObject).instance();

    expect(DashboardCallsWidgetInstance.renderCharts()).toMatchSnapshot();
});

it('renderNoOffersPlaceholder: должен вернуть DashboardWidgetAlert c корретными параметрами', () => {
    const DashboardCallsWidgetInstance = shallow(
        <DashboardCallsWidget
            bunkerDict={ bunkerDict }
            hasCallTracking={ true }
            hasStats={ false }
            hasOffers={ false }
        />, contextObject).instance();

    expect(DashboardCallsWidgetInstance.renderNoOffersPlaceholder()).toMatchSnapshot();
});

it('renderNoStatsPlaceholder: должен вернуть DashboardWidgetAlert c корретными параметрами', () => {
    const DashboardCallsWidgetInstance = shallow(<DashboardCallsWidget
        bunkerDict={ bunkerDict }
        hasCallTracking={ true }
        hasStats={ false }
        hasOffers={ true }
    />, contextObject).instance();

    expect(DashboardCallsWidgetInstance.renderNoStatsPlaceholder()).toMatchSnapshot();
});

it('renderNoCallTrackingPlaceholder: должен вернуть DashboardWidgetAlert c корретными параметрами', () => {
    const DashboardCallsWidgetInstance = shallow(<DashboardCallsWidget
        bunkerDict={ bunkerDict }
        hasCallTracking={ false }
    />, contextObject).instance();

    expect(DashboardCallsWidgetInstance.renderNoCallTrackingPlaceholder()).toMatchSnapshot();
});
