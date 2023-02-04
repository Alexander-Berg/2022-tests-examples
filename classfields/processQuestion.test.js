jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const fs = require('fs');
const path = require('path');

const questionsCsv = fs.readFileSync(path.resolve(__dirname, '../mocks/questions.csv'), 'utf8');

const getResource = require('auto-core/react/lib/gateApi').getResource;

const processQuestion = require('./processQuestion');

const { AUTOGURU_SET_CURRENT_QUESTION } = require('../actionTypes');

const responseGroupsCount = (count) => Promise.resolve({
    grouping: {
        groups_count: count,
    },
});

const gateApiMock = (resource, params) => {
    if (resource === 'searchCount') {
        if (params.search_tag && params.search_tag.includes('big-trunk')) {
            return responseGroupsCount(0);
        }
        if ((params.catalog_filter && params.catalog_filter.includes('mark=AUDI,model=A4')) ||
            (params.exclude_catalog_filter && params.exclude_catalog_filter.includes('mark=AUDI,model=A4'))
        ) {
            return responseGroupsCount(10);
        }
        return responseGroupsCount(10);
    }
};
getResource.mockImplementation(gateApiMock);

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

it('не должен пропустить вопрос, у которого после проверки остается больше одного варианта ответа', async() => {
    await store.dispatch(processQuestion(2, [ [ 'От=10000', 'До=100000000' ], [] ]));
    const actions = store.getActions();
    const updateQuestionAction = actions.find((action) => action.type === AUTOGURU_SET_CURRENT_QUESTION);
    expect(updateQuestionAction.payload.questionIndex).toBe(2);
});

it('должен пропустить вопрос, у которого после проверки остается один вариант ответа', async() => {
    await store.dispatch(processQuestion(1, [ [ 'От=10000', 'До=100000000' ] ]));
    const actions = store.getActions();
    const updateQuestionAction = actions.find((action) => action.type === AUTOGURU_SET_CURRENT_QUESTION);
    expect(updateQuestionAction.payload.questionIndex).toBe(2);
});

it('должен пропустить два вопроса, если у пропускаемого варианта ответа отмечен пропуск следующего вопроса', async() => {
    await store.dispatch(processQuestion(3, [ [ 'От=10000', 'До=100000000' ], [], [] ]));
    const actions = store.getActions();
    const updateQuestionAction = actions.find((action) => action.type === AUTOGURU_SET_CURRENT_QUESTION);
    expect(updateQuestionAction.payload.questionIndex).toBe(5);
});
