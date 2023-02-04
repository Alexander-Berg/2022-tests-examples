const React = require('react');
const { shallow } = require('enzyme');

const CardGroupFilterComplectationAdditionalOptions = require('./CardGroupFilterComplectationAdditionalOptions');

const OPTIONS = [
    {
        name: 'опция 1',
        code: '',
    },
    {
        name: 'опция 2',
        code: 'option2',
    },
    {
        name: 'опция 3 c очень длинным  названием',
        code: 'option3',
        group: 'group1',
    },
    {
        name: 'опция 4, у которой название длиннее чем у опции 3',
        code: 'option4',
        group: 'group1',
    },
];

const OPTIONS_GROUPED = [
    {
        groupName: 'Группа 1',
        options: [
            {
                name: 'опция 1',
                code: '',
                group: 'Группа 1',
            },
            {
                name: 'опция 2',
                code: 'option2',
                group: 'Группа 1',
            },
        ],
    },
    {
        groupName: 'Группа 2',
        options: [
            {
                name: 'опция 3 c очень длинным  названием',
                code: 'option3',
                group: 'Группа 2',
            },
            {
                name: 'опция 4, у которой название длиннее чем у опции 3',
                code: 'option4',
                group: 'Группа 2',
            },
        ],
    },
];

it('при клике на не выбранную ранее опцию, должен добавить ее код в список значений и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterComplectationAdditionalOptions
            flatOptions={ OPTIONS }
            optionsGrouped={ OPTIONS_GROUPED }
            values={ [ 'option1' ] }
            onChange={ onChangeMock }
        />,
    );

    const uncheckedItem = tree.find({ value: 'option2' }).at(0).dive().find('input');
    uncheckedItem.simulate('change');

    expect(onChangeMock).toHaveBeenCalledWith([ 'option1', 'option2' ]);
});

it('при клике на не выбранную ранее опцию из группы опций должен добавить ее код к имеющимся опциям ее группы и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterComplectationAdditionalOptions
            flatOptions={ OPTIONS }
            optionsGrouped={ OPTIONS_GROUPED }
            values={ [ 'option1', 'option3' ] }
            onChange={ onChangeMock }
        />,
    );

    const uncheckedItem = tree.find({ value: 'option4' }).at(0).dive().find('input');
    uncheckedItem.simulate('change');

    expect(onChangeMock).toHaveBeenCalledWith([ 'option1', 'option3,option4' ]);
});

it('при клике на выбранную ранее опцию, должен убрать ее код из списка значений и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterComplectationAdditionalOptions
            flatOptions={ OPTIONS }
            optionsGrouped={ OPTIONS_GROUPED }
            values={ [ 'option1', 'option2' ] }
            onChange={ onChangeMock }
        />,
    );

    const checkedItem = tree.find({ value: 'option2' }).at(0).dive().find('input');
    checkedItem.simulate('change');

    expect(onChangeMock).toHaveBeenCalledWith([ 'option1' ]);
});

it('при клике на выбранную ранее опцию из группы опций, должен убрать ее код из списка значений группы и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterComplectationAdditionalOptions
            flatOptions={ OPTIONS }
            optionsGrouped={ OPTIONS_GROUPED }
            values={ [ 'option1', 'option3,option4' ] }
            onChange={ onChangeMock }
        />,
    );

    const checkedItem = tree.find({ value: 'option3' }).at(0).dive().find('input');
    checkedItem.simulate('change');

    expect(onChangeMock).toHaveBeenCalledWith([ 'option1', 'option4' ]);
});
