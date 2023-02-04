const React = require('react');
const { shallow } = require('enzyme');

const {
    BarChart,
    ResponsiveContainer,
} = require('recharts');

const BarChartHorizontal = require('./BarChartHorizontal');

const DATA = [
    { value: 10, label: '10 шт.', name: 'Магнитофоны импортные' },
    { value: 40, label: '40 шт.', name: 'Куртки замшевые' },
    { value: 51, label: '> 50', name: 'Портсигары' },
];

const COLORS = {
    graph: '#B0E8F0',
    text: '#6FAEB7',
};

it('должен правильно вычислять длину лейблов и устанавливать паддинг для граффика', () => {
    const tree = shallow(
        <BarChartHorizontal
            data={ DATA }
            color={ COLORS }
        />,
    );

    const barChart = tree.find(BarChart);
    const margin = barChart.props().margin;

    expect(margin.right).toBe(68);
});

it('должен правильно вычислять высоту графиков', () => {
    const tree = shallow(
        <BarChartHorizontal
            data={ DATA }
            color={ COLORS }
        />,
    );

    const container = tree.find(ResponsiveContainer);

    const heightProp = container.props().height;

    expect(heightProp).toEqual(68);
});
