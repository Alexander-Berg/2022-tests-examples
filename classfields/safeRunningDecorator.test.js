const safeRunningDecorator = require('./safeRunningDecorator');

const MOCK_DATA = 'MOCK_DATA';

const mockMethod = function() {
    return MOCK_DATA;
};

const mockFailedMethod = function() {
    /* eslint-disable-next-line no-undef */
    return unknownObject;
};

const consoleErrorOriginal = console.error;

beforeEach(() => {
    /* eslint no-console: 0*/
    console.error = jest.fn(() => {});
});

afterEach(() => {
    /* eslint no-console: 0*/
    console.error = consoleErrorOriginal;
});

describe('декоратор безопасного запуска', () => {
    it('декорированный метод успешно возвращает данные', () => {
        expect(safeRunningDecorator(mockMethod)()).toEqual(MOCK_DATA);
    });

    it('метод, декорированный в составе объекта, успешно возвращает данные', () => {
        const util = {
            one: mockMethod,
            two: mockMethod,
        };

        expect(safeRunningDecorator(util).two()).toEqual(MOCK_DATA);
    });

    it('нормально отрабатывает для падающего метода', () => {
        expect(safeRunningDecorator(mockFailedMethod)()).toBeUndefined();
    });

    it('нормально отрабатывает если передан объект с падающими методами', () => {
        const util = {
            one: mockFailedMethod,
            two: mockFailedMethod,
        };

        expect(safeRunningDecorator(util).two()).toBeUndefined();
    });

    it('если передан некоректный объект, возвращает его в исходном виде', () => {
        const incorrectObject = [];

        expect(safeRunningDecorator(incorrectObject)).toEqual(incorrectObject);
    });
});
