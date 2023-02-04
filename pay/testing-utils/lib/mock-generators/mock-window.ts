import { getWindowMock } from './get-window-mock';

type MockStrategy = 'top.opener' | 'window.open';

export function mockWindow(prop: MockStrategy) {
    const windowMock = getWindowMock();

    interface PartialWindow {
        close?: Window['close'];
        focus?: Window['focus'];
        postMessage?: Window['postMessage'];
        closed?: Window['closed'];
        top?: PartialWindow;
        opener?: PartialWindow;
    }

    switch (prop) {
        case 'top.opener':
            jest.spyOn(window as PartialWindow, 'top', 'get').mockImplementation(() => ({
                opener: windowMock,
            }));
            break;
        case 'window.open':
            global.open = jest.fn().mockName('window.open').mockReturnValue(windowMock);
            break;
    }

    return windowMock;
}
