const React = require('react');
const { shallow } = require('enzyme');

const ChartStacked = require('./ChartStacked');
const counters = require('./mocks/counters');
const dailyCounters = require('./mocks/dailyCounters');

it('должен вернуть компонент ChartStacked', () => {
    const ChartStackedInstance = shallow(<ChartStacked counters={ counters } dailyCounters={ dailyCounters }/>).instance();
    ChartStackedInstance.renderChart = () => 'chart';

    expect(ChartStackedInstance.render()).toMatchSnapshot();
});

it('onItemMouseEnter: должен установить корректный state', () => {
    const ChartStackedInstance = shallow(<ChartStacked
        counters={ counters }
        dailyCounters={ dailyCounters }
    />).instance();
    ChartStackedInstance.setState = jest.fn();
    ChartStackedInstance.onItemMouseEnter({
        currentTarget: {
            getAttribute: () => 0,
        },
    });

    expect(ChartStackedInstance.setState).toHaveBeenCalledWith({
        popupTitle: '11 декабря, среда',
        popupCounters: {
            cardView: 1,
            phoneShow: 7,
            favorite: 3,
            cardViewPhoneShowConversion: 0.23,
        },
    });
});
