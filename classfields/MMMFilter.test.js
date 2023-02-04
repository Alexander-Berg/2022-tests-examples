const React = require('react');
const { shallow } = require('enzyme');

const MMMFilter = require('./MMMFilter');

const marks = [
    { id: 'MARK1', name: 'mark1' },
    { id: 'MARK2', name: 'mark2' },
];

const models = [
    { id: 'A1', name: 'a1' },
    { id: 'B2', name: 'b2', nameplates: [ { id: '1', name: 'odin' }, { id: '2', name: 'dva' } ] },
    { id: 'C3', name: 'c3' },
];

const generations = [
    { id: 'A1', name: 'a1', generations: [ { id: '1', name: 'odin' }, { id: '2', name: 'dva' }, { id: '3', name: 'tri' } ] },
    { id: 'B2', name: 'b1', generations: [ { id: '4', name: 'четвертый' }, { id: '5', name: 'пятый' } ] },
];
const onChange = jest.fn();

it('должен правильно определить выбранную марку', () => {
    const wrapper = shallow(
        <MMMFilter
            index={ 0 }
            marks={ marks }
            models={ models }
            generations={ generations }
            section="new"
            selected={{ mark: 'AUDI' }}
            onChange={ onChange }
            withGeneration={ true }
            withVendors={ true }
        />,
    );
    expect(wrapper.find('MarkFilter').props().value).toBe('AUDI');
});

it('должен правильно определить выбранную марку, если это вендор', () => {
    const wrapper = shallow(
        <MMMFilter
            index={ 0 }
            marks={ marks }
            models={ models }
            generations={ generations }
            section="new"
            selected={{ vendor: 'VENDOR1' }}
            onChange={ onChange }
            withGeneration={ true }
            withVendors={ true }
        />,
    );
    expect(wrapper.find('MarkFilter').props().value).toBe('VENDOR1');
});

it('не должно быть фильтра поколений в new', () => {
    const wrapper = shallow(
        <MMMFilter
            index={ 0 }
            marks={ marks }
            models={ models }
            generations={ generations }
            section="new"
            selected={{ vendor: 'VENDOR1' }}
            onChange={ onChange }
            withGeneration={ true }
            withVendors={ true }
        />,
    );
    expect(wrapper.find('GenerationFilter').isEmptyRender()).toBe(true);
});

it('должен быть фильтр поколений в used', () => {
    const wrapper = shallow(
        <MMMFilter
            index={ 0 }
            marks={ marks }
            models={ models }
            generations={ generations }
            section="used"
            selected={{ vendor: 'VENDOR1' }}
            onChange={ onChange }
            withGeneration={ true }
            withVendors={ true }
        />,
    );
    expect(wrapper.find('GenerationFilter').isEmptyRender()).toBe(false);
});
