jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
    setItem: jest.fn(),
}));

import MockDate from 'mockdate';

import { MINUTE, SECOND } from 'auto-core/lib/consts';

import { getItem, setItem } from 'auto-core/react/lib/localstorage';

import { retrieveSessionData, updateSessionData, LS_KEY } from './userSession';

const getItemMock = getItem as jest.MockedFunction<typeof getItem>;

const defaultSession = {
    autoPopupShown: false,
    openedAt: 1587340800000,
    updatedAt: 1587340800000,
    visits: 0,
};
const currentDate = '2020-04-20';

beforeEach(() => {
    MockDate.set(currentDate);
});

afterEach(() => {
    MockDate.reset();
});

describe('retrieveSessionData()', () => {
    it('если в сторадже ничего нет, вернет дефолтный объект', () => {
        getItemMock.mockReturnValueOnce(null);

        const result = retrieveSessionData();

        expect(result).toEqual(defaultSession);
    });

    it('если в сторадже невалидные данные, вернет дефолтный объект', () => {
        getItemMock.mockReturnValueOnce(JSON.stringify({ foo: 'bar' }));

        const result = retrieveSessionData();

        expect(result).toEqual(defaultSession);
    });

    it('если в сторадже есть открытая сессия, вернет ее объект', () => {
        const data = {
            autoPopupShown: true,
            openedAt: 123,
            updatedAt: 123,
            visits: 42,
        };
        getItemMock.mockReturnValueOnce(JSON.stringify(data));

        const result = retrieveSessionData();

        expect(result).toEqual(data);
    });
});

describe('updateSessionData()', () => {
    it('увеличит кол-во визитов и сбросить флаг автопопапа, если сессия была обновлена раньше, чем 30 минут назад', () => {
        const updatedAt = new Date(currentDate).getTime() - 31 * MINUTE;
        const data = {
            autoPopupShown: true,
            openedAt: updatedAt,
            updatedAt: updatedAt,
            visits: 0,
        };
        getItemMock.mockReturnValueOnce(JSON.stringify(data));

        updateSessionData();

        const now = Date.now();
        const expectedArg = JSON.stringify({ openedAt: now, updatedAt: now, visits: 1, autoPopupShown: false });
        expect(setItem).toHaveBeenCalledTimes(1);
        expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
    });

    it('увеличит кол-во визитов и сбросить флаг автопопапа, если сессия была обновлена раньше, чем переданный таймаут', () => {
        const timeout = 30;
        const updatedAt = new Date(currentDate).getTime() - timeout * SECOND - 1;
        const data = {
            autoPopupShown: true,
            openedAt: updatedAt,
            updatedAt: updatedAt,
            visits: 0,
        };
        getItemMock.mockReturnValueOnce(JSON.stringify(data));

        updateSessionData({ timeout });

        const now = Date.now();
        const expectedArg = JSON.stringify({ openedAt: now, updatedAt: now, visits: 1, autoPopupShown: false });
        expect(setItem).toHaveBeenCalledTimes(1);
        expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
    });

    it('проапдейтит текущие данные, если сессия была обновлена позже, чем 30 минут назад', () => {
        const updatedAt = new Date(currentDate).getTime() - 29 * MINUTE;
        const data = {
            autoPopupShown: false,
            openedAt: updatedAt,
            updatedAt: updatedAt,
            visits: 42,
        };
        getItemMock.mockReturnValueOnce(JSON.stringify(data));

        updateSessionData({ autoPopupShown: true });

        const now = Date.now();
        const expectedArg = JSON.stringify({ autoPopupShown: true, openedAt: updatedAt, updatedAt: now, visits: 42 });
        expect(setItem).toHaveBeenCalledTimes(1);
        expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
    });

    it('не будет трогать текущий флаг автопопапа при апдейте, если новое значение не передано', () => {
        const updatedAt = new Date(currentDate).getTime() - 29 * MINUTE;
        const data = {
            autoPopupShown: false,
            openedAt: updatedAt,
            updatedAt: updatedAt,
            visits: 42,
        };
        getItemMock.mockReturnValueOnce(JSON.stringify(data));

        updateSessionData();

        const now = Date.now();
        const expectedArg = JSON.stringify({ autoPopupShown: false, openedAt: updatedAt, updatedAt: now, visits: 42 });
        expect(setItem).toHaveBeenCalledTimes(1);
        expect(setItem).toHaveBeenCalledWith(LS_KEY, expectedArg);
    });
});
