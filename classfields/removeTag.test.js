/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const removeTag = require('./removeTag');
const gateApi = require('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const withCalls = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock');

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve());
});

it('должен вызвать getResource с параметрами тега и звонка', () => {
    const store = mockStore({
        calls: withCalls,
    });

    store.dispatch(
        removeTag({ call_id: 'call_id_1' }, 'tag_1'),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('removeCallTags', {
        dealer_id: undefined,
        call_id: 'call_id_1',
        tags: [ 'tag_1' ],
    });
});

it('должен обновить теги у звонка', () => {
    expect.assertions(1);
    const store = mockStore({
        calls: withCalls,
    });

    store.dispatch(
        removeTag(withCalls.callsList.calls[0], { value: 'кредит' }),
    );

    const tags = store.getActions()[0].payload.calls[0].tags;

    expect(tags).toEqual([
        {
            value: 'трейдин',
        },
    ]);
});

it('должен ревертить теги к предыдущему состоянию, если ресурс ответил ошибкой', () => {
    expect.assertions(1);

    gateApi.getResource.mockImplementation(() => Promise.reject());

    const store = mockStore({
        calls: withCalls,
    });

    return store.dispatch(
        removeTag(withCalls.callsList.calls[0], { value: 'кредит' }),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            const tags = store.getActions()[1].payload.calls[0].tags;

            expect(tags).toEqual([
                {
                    value: 'кредит',
                },
                {
                    value: 'трейдин',
                },
            ]);
        },
    );
});
