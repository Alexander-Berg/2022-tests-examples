const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CardGroupFilterMultiSelect = require('./CardGroupFilterMultiSelect');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ITEMS = [
    {
        id: '1.6 л / 110 л.с. / Бензин',
        title: 'Бензин, 1.6, 110 л.с.',
        value: [ '1', '2' ],
    },
    {
        id: '1.6 л / 80 л.с. / Бензин',
        title: 'Бензин, 1.6, 80 л.с.',
        value: [ '3' ],
    },
    {
        id: '1.6 л / 100 л.с. / Дизель',
        title: 'Дизель, 1.6, 100 л.с.',
        value: [ '4' ],
    },
];

it('должен корректно отобразить элементы, у которых value - это массив значений', () => {

    const tree = shallow(
        <CardGroupFilterMultiSelect
            items={ ITEMS }
            values={ [ '1', '2', '3' ] }
            onChange={ _.noop }
        />,
        { context: contextMock },
    );

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен передать в onChange плоский список значений', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterMultiSelect
            items={ ITEMS }
            values={ [] }
            onChange={ onChangeMock }
        />,
        { context: contextMock },
    );

    tree.find('Select').simulate('change', [ '1,2' ]);

    expect(onChangeMock).toHaveBeenCalledWith([ '1', '2' ]);
});
