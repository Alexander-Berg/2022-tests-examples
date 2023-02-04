const React = require('react');
const TonnageFromToFilter = require('./TonnageFromToFilter');
const { shallow } = require('enzyme');

it('Правильно отрендерится с trucksCategory=LCV', () => {
    expect(shallow(<TonnageFromToFilter to={ 1000 }/>).dive()).toMatchSnapshot();
});

it('Правильно отрендерится с trucksCategory=TRUCK', () => {
    expect(shallow(<TonnageFromToFilter to={ 3500 } trucksType="TRUCK"/>).dive()).toMatchSnapshot();
});

it('Выделен правильный Radio в RadioGroup', () => {
    const wrapper = shallow(
        <TonnageFromToFilter to={ 1000 } trucksType="LCV"/>,
    ).dive();

    expect(wrapper.find('Radio[value="-1000"]').props().checked).toEqual(true);
});

it('Ни один Radio не выделен', () => {
    const wrapper = shallow(
        <TonnageFromToFilter trucksType="LCV"/>,
    ).dive();

    expect(wrapper.find('Radio[value="-1000"]').props().checked).toEqual(false);
    expect(wrapper.find('Radio[value="1000-1500"]').props().checked).toEqual(false);
    expect(wrapper.find('Radio[value="1500-"]').props().checked).toEqual(false);
});

it('При изменении отправляет правильные параметры', () => {
    const onChange = jest.fn();
    const wrapper = shallow(
        <TonnageFromToFilter onChange={ onChange }/>,
    ).dive();
    wrapper.find('Radio[value="-1000"]').simulate('check', null, { value: '-1000' });
    expect(onChange).toHaveBeenCalledWith({ from: 0, to: 1000 }, { name: 'loading_range' });
});
