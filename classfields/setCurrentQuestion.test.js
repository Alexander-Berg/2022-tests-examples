const fs = require('fs');
const path = require('path');

const questionsCsv = fs.readFileSync(path.resolve(__dirname, '../mocks/questions.csv'), 'utf8');

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
const getResource = require('auto-core/react/lib/gateApi').getResource;

const setCurrentQuestion = require('./setCurrentQuestion');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

let store;

beforeEach(() => {
    store = mockStore({
        bunker: {
            'autoguru/questions': questionsCsv,
        },
        autoguru: {
            questionIndex: 0,
            answerValues: [],
            questions: [],
            isPending: false,
        },
    });
});

const responseWithCount = (count) => Promise.resolve({
    grouping: {
        groups_count: count,
    },
});

it('должен сохранить текущие ответы, обновить список вопросов и установить новый номер вопроса, если количество групп > 0', async() => {
    const gateApiMock = () => responseWithCount(10);
    getResource.mockImplementation(gateApiMock);
    await store.dispatch(setCurrentQuestion(1, [ [] ], [ { answer: 'измененный ответ1' }, { answer: 'измененный ответ2' } ]));
    const actions = store.getActions();
    expect(actions).toMatchSnapshot();
});

it('должен передать ошибку, если количество групп == 0', async() => {
    const gateApiMock = () => responseWithCount(0);
    getResource.mockImplementation(gateApiMock);
    await store.dispatch(setCurrentQuestion(1, [ [] ], [ { answer: 'измененный ответ1' }, { answer: 'измененный ответ2' } ]));
    const actions = store.getActions();
    expect(actions).toMatchSnapshot();
});
