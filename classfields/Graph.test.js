const React = require('react');
const { shallow } = require('enzyme');

const { YAxis } = require('recharts');

const Graph = require('./Graph');

const TEST_CASES = [
    {
        input: [
            { date: '2019-10-24', value1: 100, value2: 150 },
            { date: '2019-10-24', value1: 120, value2: 170 },
        ],
        output: 28.5,
        description: 'до 1000',
    },
    {
        input: [
            { date: '2019-10-24', value1: 999, value2: 2 },
            { date: '2019-10-24', value1: 120, value2: 170 },
        ],
        output: 43.5,
        description: 'от 1 000 до 10 000',
    },
    {
        input: [],
        output: 0,
        description: 'без data',
    },
];

describe('должен правильно вычислять ширину лейбла для оси Y для значений', () => {
    TEST_CASES.forEach((testCase) => {
        it(testCase.description, () => {
            const tree = shallow(
                <Graph
                    data={ testCase.input }
                    configs={ [
                        { dataKey: 'value1', color: '#5E9CE0', name: 'Бордюры' },
                        { dataKey: 'value2', color: '#9ED1D8', name: 'Поребрики' },
                    ] }
                />,
            );

            const labels = tree.find(YAxis);
            const widthProp = labels.props().width;

            expect(widthProp).toBe(testCase.output);
        });
    });
});
