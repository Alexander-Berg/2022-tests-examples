const React = require('react');
const _ = require('lodash');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const questionMock = require('auto-core/react/dataDomain/autoguru/mocks/questions.mock');

const AutoGuru = require('./AutoGuruDumb');

const BASE_PROPS = {
    questions: questionMock,
    onAnswer: _.noop,
    onReset: _.noop,
    answerValues: [],
    readyTextLines: [],
    onInit: _.noop,
    onShowListing: _.noop,
};

it('должен отрендерить страницу вопроса - первый вопрос, без футера', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 0 }
            question={ questionMock[ 0 ] }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить страницу вопроса - второй вопрос, с футером', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 1 }
            question={ questionMock[ 1 ] }
            groupsCount={ 20 }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить итоговую страницу, если вопросы закончились', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 6 }
            question={ null }
            groupsCount={ 20 }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить итоговую страницу, если количество групп менее 5', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 2 }
            question={ questionMock[ 2 ] }
            groupsCount={ 4 }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отправить метрику с пользовательским значением ответа, если пользователь выбрал ответ', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 0 }
            question={ questionMock[ 0 ] }
            groupsCount={ 10 }
        />,
        { context: contextMock },
    );
    tree.instance().handleAnswer({ answer: 'От', search_param: 'price_from', price_from: 1000000, default_value: 50000 });
    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'guru', 'questions', '1. Вопрос с пользовательским вводом', 'От 1000000' ]);
});

it('должен отправить метрику с дефолтным значением ответа, если пользователь ответ не выбрал', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 0 }
            question={ questionMock[ 0 ] }
            groupsCount={ 10 }
        />,
        { context: contextMock },
    );
    tree.instance().handleAnswer({ answer: 'От', search_param: 'price_from', price_from: null, default_value: 50000 });
    expect(contextMock.metrika.sendPageEvent).toHaveBeenLastCalledWith([ 'guru', 'questions', '1. Вопрос с пользовательским вводом', 'От 50000' ]);
});

it('должен остаться на текущем вопросе, если вопросы закончились и передан параметр showListingOnLastAnswer', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 6 }
            question={ null }
            groupsCount={ 20 }
            showListingOnLastAnswer={ true }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен остаться на текущем вопросе, если количество групп менее 5 и передан параметр showListingOnLastAnswer', () => {
    const tree = shallow(
        <AutoGuru
            { ...BASE_PROPS }
            questionIndex={ 2 }
            question={ questionMock[ 2 ] }
            groupsCount={ 4 }
            showListingOnLastAnswer={ true }
        />,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

describe('фейковый лоадер', () => {
    let tree;

    beforeEach(() => {
        tree = shallow(
            <AutoGuru
                { ...BASE_PROPS }
                questionIndex={ 2 }
                question={ questionMock[ 2 ] }
                groupsCount={ 5 }
                onShowListing={ _.noop }
            />,
            { context: contextMock },
        );
        jest.useFakeTimers();
    });

    it('должен отрендерить лоадер при переходе к итоговой странице', () => {
        tree.setProps({ groupsCount: 4 });
        expect(tree.find('.AutoGuru__imageWrapper')).toMatchSnapshot();
    });

    it('должен показать обычное изображение после истечения таймаута', () => {
        tree.setProps({ groupsCount: 4 });
        jest.runAllTimers();
        expect(tree.find('.AutoGuru__imageWrapper')).toMatchSnapshot();
    });
});
