/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const ClientsItemChart = require('./ClientsItemChart');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const dayjs = require('auto-core/dayjs');

it('должен вернуть компонент графика', () => {
    const start = dayjs('2020-07-05').subtract(3, 'day');
    const end = dayjs('2020-07-05');

    const dailyStats = [
        {
            date: '2020-07-05',
            product_stats: [ { count: 1, product: 'match-application:cars:new', sum: 700 } ],
            total: { count: 1, sum: 700 },
        },
    ];

    const clientsItemChart = shallowToJson(shallow(
        <ClientsItemChart
            start={ start }
            end={ end }
            dailyStats={ dailyStats }
            isFreezed={ true }
        />,
    ));

    expect(clientsItemChart).toMatchSnapshot();
});

it('должен вернуть компонент tooltip', () => {
    const start = dayjs('2020-07-05').subtract(3, 'day');
    const end = dayjs('2020-07-05');

    const dailyStats = [
        {
            date: '2020-07-05',
            product_stats: [ { count: 1, product: 'match-application:cars:new', sum: 700 } ],
            total: { count: 1, sum: 700 },
        },
    ];

    const clientsItemChartInstance = shallow(
        <ClientsItemChart
            start={ start }
            end={ end }
            dailyStats={ dailyStats }
            isFreezed={ true }
        />,
    ).instance();

    expect(
        clientsItemChartInstance.renderTooltip({ payload: [ { payload: { value: '700', date: '2020-07-05' } } ] }),
    ).toMatchSnapshot();
});
