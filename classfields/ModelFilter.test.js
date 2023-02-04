const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ModelFilter = require('./ModelFilter');

const items = [
    { id: 'A1', name: 'a1' },
    { id: 'B2', name: 'b2', nameplates: [ { id: '1', name: 'odin', semantic_url: 'a1' }, { id: '2', name: 'dva', semantic_url: 'a2' } ] },
    { id: 'C3', name: 'c3' },
];
const onChange = jest.fn();
const value = [ { id: 'B2', nameplates: [ '1', '2' ], generations: [ '111', '222' ] } ];

it('должен правильно вычислить пункты для селекта', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    expect(shallowToJson(wrapper.find('Select').children())).toMatchSnapshot();
});

it('должен правильно вычислить значения селекта из пропов', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    expect(wrapper.find('Select').props().value).toEqual([ 'B2#1', 'B2#2' ]);
});

it('должен правильно обрабатывать добавление модели', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    wrapper.find('Select').simulate('change', [ 'B2#1', 'B2#2', 'A1' ]);
    expect(onChange).toHaveBeenCalledWith([ 'B2#1', 'B2#2', 'A1' ], { mode: 'add', name: 'model', value: [ 'A1' ] });
});

it('должен правильно обрабатывать удаление модели', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    wrapper.find('Select').simulate('change', [ 'B2#1' ]);
    expect(onChange).toHaveBeenCalledWith([ 'B2#1' ], { mode: 'delete', name: 'model', value: [ 'B2#2' ] });
});

it('должен правильно обрабатывать сброс моделей', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    wrapper.find('Select').simulate('change', []);
    expect(onChange).toHaveBeenCalledWith([], { mode: 'clear', name: 'model', value: [] });
});

it('должен правильно обрабатывать смену модели', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ [ { id: 'A1', nameplates: [], generations: [] } ] }
            withNameplates={ true }
        />,
    );
    wrapper.find('Select').simulate('change', [ 'B2' ]);
    expect(onChange).toHaveBeenCalledWith([ 'B2' ], { mode: 'change', name: 'model', value: [ 'B2' ] });
});

it('должен правильно установить вид селекта для мультивыбора', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            multiModels={ true }
            onChange={ onChange }
            value={ [ { id: 'A1', nameplates: [], generations: [] } ] }
            withNameplates={ true }
        />,
    );
    expect(wrapper.find('Select').props().mode).toBe('check');
});

it('должен правильно установить вид селекта без мультивыбора', () => {
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            multiModels={ false }
            onChange={ onChange }
            value={ [ { id: 'A1', nameplates: [], generations: [] } ] }
            withNameplates={ true }
        />,
    );
    expect(wrapper.find('Select').props().mode).toBe('radio-check');
});

it('не должен отобразить имя модели перед шильдом, если у шильда есть параметр no_model', () => {
    const items = [
        { id: 'B2', name: 'b2', nameplates: [ { id: '1', name: 'odin', semantic_url: 'a1', no_model: true } ] },
    ];
    const wrapper = shallow(
        <ModelFilter
            items={ items }
            onChange={ onChange }
            value={ value }
            withNameplates={ true }
        />,
    );
    expect(shallowToJson(wrapper.find('Select').children())).toMatchSnapshot();
});
