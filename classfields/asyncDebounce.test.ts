import asyncDebounce from './asyncDebounce';

const mockFunction = jest.fn(() => Promise.resolve({}));

describe('асинхронный debounce -', () => {
    it('функция-аргумент не вызывается сразу', async() => {
        const result = asyncDebounce(mockFunction, 1000);
        const res = result();
        expect(mockFunction).toHaveBeenCalledTimes(0);
        await res;
    });

    it('await', async() => {
        const result = asyncDebounce(mockFunction, 1000);
        await result();
        expect(mockFunction).toHaveBeenCalledTimes(1);
    });
});
