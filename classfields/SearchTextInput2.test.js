const React = require('react');
const { shallow } = require('enzyme');

const SearchTextInput2 = require('./SearchTextInput2');

it('должен менять стейт при фокусе', () => {
    const wrapper = shallow(
        <SearchTextInput2 onCancelClick={ () => {} }/>,
    );
    wrapper.find('TextInput').simulate('focusChange', true);
    expect(wrapper.state().focused).toBe(true);
});

it('не должен менять стейт при потере фокуса, когда есть onCancelClick', () => {
    const wrapper = shallow(
        <SearchTextInput2 onCancelClick={ () => {} }/>,
    );
    wrapper.find('TextInput').simulate('focusChange', true);
    wrapper.find('TextInput').simulate('focusChange', false);
    expect(wrapper.state().focused).toBe(true);
});

it('должен вызвать onCancelClick и поменять стейт при нажатии на отмену', () => {
    const onCancelClick = jest.fn();
    const wrapper = shallow(
        <SearchTextInput2 onCancelClick={ onCancelClick }/>,
    );
    wrapper.find('TextInput').simulate('focusChange', true);
    wrapper.find('.SearchTextInput__cancel').simulate('click');
    expect(wrapper.state().focused).toBe(false);
    expect(onCancelClick).toHaveBeenCalled();
});
