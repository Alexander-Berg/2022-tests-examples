const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const AutoGuruQuestion = require('./AutoGuruQuestion');

it('должен отрендерить вопрос с пользовательским вводом', () => {
    const question = {
        answers: [],
        answers_type: 'input',
        question: 'Вопрос',
    };
    const tree = shallow(
        <AutoGuruQuestion question={ question }/>,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить вопрос с одним вариантом ответа', () => {
    const question = {
        answers: [],
        answers_type: 'radio',
        clear_params: [],
        image_id: 'family',
        question: 'Вопрос',
    };
    const tree = shallow(
        <AutoGuruQuestion question={ question }/>,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить вопрос с несколькими вариантами ответа', () => {
    const question = {
        answers: [],
        answers_type: 'checkbox',
        question: 'Вопрос',
    };
    const tree = shallow(
        <AutoGuruQuestion question={ question }/>,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
