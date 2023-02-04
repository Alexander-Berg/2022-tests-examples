const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const MatchApplicationForm = require('./MatchApplicationForm');

it('должен отрендерить подсказку о заполнении марки в случае ее отсутствия', () => {
    const tree = shallow(
        <MatchApplicationForm
            marks={ [] }
            onSubmit={ _.noop }
            isPending={ false }
        />,
        { context: contextMock },
    );
    const hint = tree.find('.PageMatchApplication__hint');
    expect(hint.props().children).toEqual('Выберите марку');
});
