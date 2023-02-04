const React = require('react');
const { shallow } = require('enzyme');

const getColorInfo = require('auto-core/lib/util/getColorInfo');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const COLORS = [
    '040001', 'CACECB', 'FAFBFB', '97948F', '0000CC', 'EE1D19', '007F00', '200204',
].map((hex) => ({ ...getColorInfo(hex), hex }));

const CardGroupFilterColor = require('./CardGroupFilterColor');

it('при клике на не выбранный ранее цвет, должен добавить его код в список значений и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterColor
            colors={ COLORS }
            values={ [ '040001' ] }
            onChange={ onChangeMock }
        />,
        { context: contextMock },
    );

    const uncheckedItem = tree.find({ id: 'CACECB' }).at(0).dive().find('.ColorSelectorItem');
    uncheckedItem.simulate('click');

    expect(onChangeMock).toHaveBeenCalledWith([ '040001', 'CACECB' ]);
});

it('при клике на выбранный ранее цвет, должен убрать его код из списка значений и передать этот список в onChange', () => {
    const onChangeMock = jest.fn();

    const tree = shallow(
        <CardGroupFilterColor
            colors={ COLORS }
            values={ [ '040001', 'CACECB' ] }
            onChange={ onChangeMock }
        />,
        { context: contextMock },
    );

    const checkedItem = tree.find({ id: 'CACECB' }).at(0).dive().find('.ColorSelectorItem');
    checkedItem.simulate('click');

    expect(onChangeMock).toHaveBeenCalledWith([ '040001' ]);
});
