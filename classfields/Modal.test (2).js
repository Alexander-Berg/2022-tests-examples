const React = require('react');
const { shallow } = require('enzyme');

const Modal = require('./Modal');

it('должен отрендерить модал с иконкой крестика', () => {
    const tree = shallow(
        <Modal visible={ true }>foo</Modal>,
    );
    expect(tree).toMatchSnapshot();
});

it('должен отрендерить модал без крестика', () => {
    const tree = shallow(
        <Modal visible={ true } closer={ false }>foo</Modal>,
    );

    expect(tree.find('.Modal__closer').isEmptyRender()).toBe(true);
});

it('должен отрендерить контент модала без оберток', () => {
    const tree = shallow(
        <Modal visible={ true } noWrappers={ true } closer={ false }>foo</Modal>,
    );
    expect(tree.find('.Modal__container')).toMatchSnapshot();
});
