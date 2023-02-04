/**
 * Можно использовать для отладки тестов контейнеров
 * @param store
 * @returns {function(*): function(*=): *}
 */
export const loggerMiddleware = store => next => action => {
    const { type } = action;
    let direction = '';
    if (type.indexOf('REQUEST') > -1) {
        direction = '-> ';
    } else if (type.indexOf('RECEIVE') > -1) {
        direction = '<- ';
    }
    console.log(`${direction}${JSON.stringify(action)}`);
    const result = next(action);
    // console.log('next state:\n', store.getState());
    return result;
};

export const HOST = 'http://snout-test';
