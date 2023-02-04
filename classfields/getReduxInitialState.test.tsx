/**
 * @jest-environment jsdom
 */

jest.mock('auto-core/react/lib/isEnvBrowser', () => {
    return () => false;
});
jest.mock('auto-core/appConfigClient', () => {
    return { webpack: 200 };
});

import { renderToString } from 'react-dom/server';
import React from 'react';
import MockDate from 'mockdate';

import splitStoreIntoChunks from 'auto-core/react/ssr/InitialState/splitStoreIntoChunks';
import InitialState from 'auto-core/react/ssr/InitialState/InitialState';

import getReduxInitialState from './getReduxInitialState';

declare global {
    var IS_DEV: boolean;
}
beforeEach(() => {
    global.IS_DEV = false;
    MockDate.set('2022-05-25T12:00:00+03:00');
});

// это специальный e2e тест: распиливаем и склеиваем обратно
it('should split and concat RIS with empty chunks', () => {
    const store = { url1: 'https://yandex.ru', url2: 'https://yandex.ru' };
    const splittedInitialState = splitStoreIntoChunks(store);

    // проверяем, что правильно разрезали RIS
    expect(splittedInitialState).toEqual({
        chunks: [
            {
                index: 203,
                value: '{"url1":"https://yandex.ru",',
            },
            {
                index: 206,
                value: '"url2":"https://yandex.ru"}',
            },
            {
                index: 207,
                value: '',
            },
        ],
        key: 200,
    });

    // Рендерим распиленный RIS, склеиваем обратно и проверяем
    const container = document.createElement('div');
    container.innerHTML = renderToString(
        <InitialState key="initial-state" chunks={ splittedInitialState.chunks }/>,
    );
    document.body.appendChild(container);

    const reduxInitialState = getReduxInitialState();
    expect(reduxInitialState).toEqual({ url1: 'https://yandex.ru', url2: 'https://yandex.ru' });
});
