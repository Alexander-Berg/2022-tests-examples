/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/metrika-loader', () => Promise.resolve());
jest.mock('auto-core/react/lib/postMetrikPro', () => {
    return {
        postMetrikPRO: jest.fn(),
    };
});

const _ = require('lodash');

const metrikaLoader = require('auto-core/react/lib/metrika-loader');
const metrika = require('./metrika');

let state;
let originalYaMetrika;

beforeEach(() => {
    originalYaMetrika = global.Ya;

    global.Ya = {
        Metrika2: jest.fn(),
    };

    state = {
        config: {
            data: {
                metrika: [],
            },
        },
        user: {
            data: {},
        },
    };
});

afterEach(() => {
    global.Ya = originalYaMetrika;
});

describe('должен достать конфиг метрики из стора и создать инстанс метрики', () => {
    it('для одного счетчика', () => {
        state.config.data.metrika.push({ id: 'counter_id_1' });

        metrika(() => state, _.noop);

        return metrikaLoader.then(() => {
            expect(global.Ya.Metrika2).toHaveBeenCalledTimes(1);
            expect(global.Ya.Metrika2).toHaveBeenCalledWith({ id: 'counter_id_1' });
        });
    });

    it('для двух счетчиков', () => {
        state.config.data.metrika.push({ id: 'counter_id_1' });
        state.config.data.metrika.push({ id: 'counter_id_2' });

        metrika(() => state, _.noop);

        return metrikaLoader.then(() => {
            expect(global.Ya.Metrika2).toHaveBeenCalledTimes(2);
            expect(global.Ya.Metrika2).toHaveBeenCalledWith({ id: 'counter_id_1' });
            expect(global.Ya.Metrika2).toHaveBeenCalledWith({ id: 'counter_id_2' });

        });
    });
});

it('должен проставить в счетчики user_id и обновлять их при его смене в сторе', () => {
    state.config.data.metrika.push({ id: 'counter_id_1' });
    state.config.data.metrika.push({ id: 'counter_id_2' });
    state.user.data.id = 'user_id_1';

    const userIdSetter = jest.fn();

    global.Ya.Metrika2
        .mockImplementationOnce(() => ({
            id: 'counter_1',
            setUserID: userIdSetter,
        }))
        .mockImplementationOnce(() => ({
            id: 'counter_2',
            setUserID: userIdSetter,
        }));

    let subscribeCallback;
    const subscribeToStore = (callback) => {
        subscribeCallback = callback;
    };

    metrika(() => state, subscribeToStore);

    return metrikaLoader.then(() => {
        expect(userIdSetter.mock.instances[0].id).toBe('counter_1');
        expect(userIdSetter.mock.instances[1].id).toBe('counter_2');
        expect(userIdSetter).toHaveBeenCalledWith('user_id_1');

        state.user.data.id = 'user_id_2';
        subscribeCallback();

        expect(userIdSetter.mock.instances[2].id).toBe('counter_1');
        expect(userIdSetter.mock.instances[3].id).toBe('counter_2');
        expect(userIdSetter).toHaveBeenCalledWith('user_id_2');
    });
});

describe('должен вызывать для всех счетчиков метод', () => {
    let metrikaInstance;

    afterEach(() => {
        metrikaInstance = null;
    });

    const TEST_CASES = [
        { name: 'hit', caller: () => metrikaInstance.hit('/url', { param: 'foo' }), expected: [ '/url', { param: 'foo' } ] },
        { name: 'params', caller: () => metrikaInstance.params([ 'param' ]), expected: [ [ 'param' ] ] },
        { name: 'reachGoal', caller: () => metrikaInstance.reachGoal('GOAL', 'params'), expected: [ 'GOAL', 'params' ] },
    ];

    TEST_CASES.forEach((testCase) => {
        it(`"${ testCase.name }"`, () => {
            state.config.data.metrika.push({ id: 'counter_id_1' });
            state.config.data.metrika.push({ id: 'counter_id_2' });

            const method = jest.fn();

            global.Ya.Metrika2
                .mockImplementationOnce(() => ({
                    id: 'counter_1',
                    [ testCase.name ]: method,
                }))
                .mockImplementationOnce(() => ({
                    id: 'counter_2',
                    [ testCase.name ]: method,
                }));

            metrikaInstance = metrika(() => state, _.noop);

            return metrikaLoader.then(() => {
                testCase.caller();

                expect(method.mock.instances[0].id).toBe('counter_1');
                expect(method.mock.instances[1].id).toBe('counter_2');
                expect(method).toHaveBeenCalledWith(...testCase.expected);
            });
        });
    });
});

it('должен сложить события в кэш и отправить их после загрузки кода метрики', () => {
    state.config.data.metrika.push({ id: 'counter_id_1' });

    const hit = jest.fn();
    const params = jest.fn();
    const reachGoal = jest.fn();

    global.Ya.Metrika2
        .mockImplementationOnce(() => ({
            id: 'counter_1',
            hit: hit,
            params: params,
            reachGoal: reachGoal,
        }));

    const metrikaInstance = metrika(() => state, _.noop);

    metrikaInstance.hit('/url', { param: 'foo' });
    metrikaInstance.params([ 'param' ]);
    metrikaInstance.reachGoal('GOAL', 'params');

    expect(hit).not.toHaveBeenCalled();
    expect(params).not.toHaveBeenCalled();
    expect(reachGoal).not.toHaveBeenCalled();

    return metrikaLoader.then(() => {
        expect(hit).toHaveBeenCalledWith('/url', { param: 'foo' });
        expect(params).toHaveBeenCalledWith([ 'param' ]);
        expect(reachGoal).toHaveBeenCalledWith('GOAL', 'params');
    });
});
