import getScrollProgress from './getScrollProgress';

const DEFAULT_WINDOW_MOCK = {
    scrollY: 0,
    innerHeight: 1000,
    document: {
        body: {
            clientHeight: 5000,
        },
    },
};

let windowSpy: any;

beforeEach(() => {
    windowSpy = jest.spyOn(window, 'window', 'get');
});

afterEach(() => {
    windowSpy.mockRestore();
});

describe('страница больше окна', () => {
    it('0%', () => {
        windowSpy.mockImplementation(() => (DEFAULT_WINDOW_MOCK));
        expect(getScrollProgress()).toBe(0);
    });

    it('50%', () => {
        windowSpy.mockImplementation(() => ({
            ...DEFAULT_WINDOW_MOCK,
            scrollY: 2000,
        }));
        expect(getScrollProgress()).toBe(50);
    });

    it('100%', () => {
        windowSpy.mockImplementation(() => ({
            ...DEFAULT_WINDOW_MOCK,
            scrollY: 4000,
        }));
        expect(getScrollProgress()).toBe(100);
    });
});

describe('страница меньше окна', () => {
    it('scrollY: 0', () => {
        windowSpy.mockImplementation(() => ({
            ...DEFAULT_WINDOW_MOCK,
            innerHeight: 1000,
            document: {
                body: {
                    clientHeight: 500,
                },
            },
        }));
        expect(getScrollProgress()).toBe(0);
    });

    it('scrollY: 100', () => {
        windowSpy.mockImplementation(() => ({
            scrollY: 100,
            innerHeight: 1000,
            document: {
                body: {
                    clientHeight: 500,
                },
            },
        }));
        expect(getScrollProgress()).toBe(0);
    });
});
