const React = require('react');
const { shallow } = require('enzyme');

const GenerationFilter = require('./GenerationFilter');

const items = [
    { id: 'A1', name: 'a1', generations: [ { id: '1', name: 'odin' }, { id: '2', name: 'dva' }, { id: '3', name: 'tri' } ] },
    { id: 'B2', name: 'b1', generations: [ { id: '4', name: 'четвертый' }, { id: '5', name: 'пятый' } ] },
];
const onChange = jest.fn();
const value = [
    { id: 'A1', generations: [ '2' ] },
    { id: 'B2', generations: [] },
];

it('должен правильно обрабатывать выбор поколения', () => {
    const wrapper = shallow(
        <GenerationFilter
            items={ items }
            onChange={ onChange }
            value={ value }
        />,
    );
    wrapper.find('SelectButton').simulate('click', { preventDefault: () => {}, stopPropagation: () => {} });
    wrapper.find('PopupGenerationItem').at(0).simulate('check', true, { value: { model: 'A1', item: '1' } });
    expect(onChange).toHaveBeenCalledWith([ { id: 'A1', generations: [ '2', '1' ] }, { id: 'B2', generations: [] } ], { name: 'generation' });
});

it('должен правильно обрабатывать удаление поколения', () => {
    const wrapper = shallow(
        <GenerationFilter
            items={ items }
            onChange={ onChange }
            value={ value }
        />,
    );
    wrapper.find('SelectButton').simulate('click', { preventDefault: () => {}, stopPropagation: () => {} });
    wrapper.find('PopupGenerationItem').at(0).simulate('check', false, { value: { model: 'A1', item: '2' } });
    expect(onChange).toHaveBeenCalledWith([ { id: 'A1', generations: [] }, { id: 'B2', generations: [] } ], { name: 'generation' });
});

it('должен правильно обрабатывать сброс поколений', () => {
    const expectedModels = [ { id: 'A1', generations: [] }, { id: 'B2', generations: [] }, { id: 'C3', generations: [] } ];
    const wrapper = shallow(
        <GenerationFilter
            items={ [ ...items, { id: 'C3', name: 'c3', generations: [ { id: '1', name: 'odin' }, { id: '2', name: 'dva' } ] } ] }
            onChange={ onChange }
            value={ [ ...value, { id: 'C3', generations: [ '1', '2' ] } ] }
        />,
    );
    wrapper.find('SelectButton').simulate('click', { preventDefault: () => {}, stopPropagation: () => {} });
    wrapper.find('PopupGenerationsListClear').simulate('click');
    expect(onChange).toHaveBeenCalledWith(expectedModels, { name: 'generation' });
});
