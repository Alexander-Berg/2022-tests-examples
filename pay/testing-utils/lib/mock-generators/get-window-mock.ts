export function getWindowMock() {
    return {
        close: jest.fn().mockName('window.close'),
        focus: jest.fn().mockName('window.focus'),
        postMessage: jest.fn().mockName('window.postMessage'),
        closed: false,
    };
}
