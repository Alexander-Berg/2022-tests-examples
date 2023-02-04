/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import mockStore from 'autoru-frontend/mocks/mockStore';

import downloadCsv from './downloadCsv';

it('должен сохранить номера телефонов в виде csv-файла', () => {
    const store = mockStore({});
    const downloadLink = {
        click: jest.fn(),
        href: '',
        style: {
            display: '',
        },
    };
    global.Blob = jest.fn();
    global.URL.createObjectURL = () => 'someurl';
    global.document.createElement = jest.fn(() => downloadLink) as any;
    global.document.body.appendChild = jest.fn();

    store.dispatch(downloadCsv([ '+791111111111', '+792222222222' ]));

    expect(global.Blob).toHaveBeenCalledWith([ '1,+7 911 111-11-11\n', '2,+7 922 222-22-22\n' ], { type: 'text/csv;charset=utf-8,' });
    expect(store.getActions()).toEqual([ {
        type: 'NOTIFIER_SHOW_MESSAGE',
        payload: { view: 'success', message: 'Файл успешно загружен' },
    } ],
    );
});

it('должен показать сообщение об ошибке, если не передали номера телефонов', () => {
    const store = mockStore({});
    store.dispatch(downloadCsv([]));
    expect(store.getActions()).toEqual([ {
        type: 'NOTIFIER_SHOW_MESSAGE',
        payload: { view: 'error', message: 'Произошла ошибка, попробуйте ещё раз' },
    } ],
    );
});
