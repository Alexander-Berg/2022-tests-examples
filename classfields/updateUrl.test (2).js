/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const updateUrl = require('./updateUrl');

it('должен вызвать replaceState c корректными параметрами', () => {
    global.history.replaceState = jest.fn();

    updateUrl({
        param1: 'newParam1',
        param2: 'param2',
    });

    //параметры window.host, protocol, pathname определяются настройкой testURL в jest.config.js
    expect(global.history.replaceState).toHaveBeenCalledWith(
        {},
        'Заявки на TRADE-IN',
        'http://localhost/?param1=newParam1&param2=param2',
    );
});
