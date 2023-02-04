import { renderHook, act } from '@testing-library/react-hooks';

import getVehicleInfoByIdentifier from 'auto-core/react/dataDomain/garageCard/actions/getVehicleInfoByIdentifier';
import addCardByIdentifier from 'auto-core/react/dataDomain/garageCard/actions/addCardByIdentifier';

import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';
import type { GetVehicleInfoResponse } from 'auto-core/types/proto/auto/api/vin/garage/response_model';

import useAddExCar, { SearchCarStatus, AddCarStatus } from './useAddExCar';

const mockDispatch = jest.fn();

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useDispatch: () => jest.fn().mockImplementation(mockDispatch),
    };
});

jest.mock('auto-core/react/dataDomain/garageCard/actions/getVehicleInfoByIdentifier');
jest.mock('auto-core/react/dataDomain/garageCard/actions/addCardByIdentifier');

const VALID_VIN = 'X4XCW89430YG23135';
const VIN_CODE_NOT_FOUND = 'VIN_CODE_NOT_FOUND';
const VIN_CODE_INVALID = 'VIN_CODE_INVALID';
const ANY_UNKNOWN_ERROR = 'ANY_UNKNOWN_ERROR';
const IN_PROGRESS = 'IN_PROGRESS';
const CARD = { id: '123' };

describe('useAddExCar', () => {
    beforeAll(() => {
        jest.useFakeTimers();
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    it('возвращает переменные по умолчанию', () => {
        const { result } = renderHook(() => useAddExCar());

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.addCarStatus).toBe(AddCarStatus.IDLE);
        expect(result.current.value).toBe('');
        expect(result.current.error).toBe('');
        expect(typeof result.current.onChange).toBe('function');
        expect(typeof result.current.onSearchCar).toBe('function');
        expect(typeof result.current.onAddCar).toBe('function');

        expect(result.current.data).toBe(null);
    });

    it('меняет value на валидный VIN, затем выполняется поиск по VIN, возвращается card - машина с таким VIN уже есть в гаража', async() => {
        const { result, waitForNextUpdate } = renderHook(() => useAddExCar());
        const mockFn = jest.fn(() => Promise.resolve({ data: { card: '123' } } as GetVehicleInfoResponse));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockFn);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        act(() => {
            result.current.onSearchCar(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.LOADING);
        expect(mockFn).toHaveBeenCalledWith(VALID_VIN);

        await waitForNextUpdate();

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.SUCCESS);
        expect(result.current.data).toEqual({ data: { card: '123' } });
    });

    it('меняет value на невалидный VIN, выполняется поиск по VIN (запрос не выполняется), в поле error записывается сообщение об ошибке', async() => {
        const { result } = renderHook(() => useAddExCar());
        const INVALID_VIN = VALID_VIN.slice(0, 10);
        const mockFn = jest.fn(() => Promise.resolve({ data: { card: '123' } } as GetVehicleInfoResponse));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockFn);

        act(() => {
            result.current.onChange(INVALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(INVALID_VIN);
        expect(result.current.error).toBe('');

        act(() => {
            result.current.onSearchCar(INVALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.error).toBe('Введите правильный VIN');
        expect(mockFn).not.toHaveBeenCalledWith(INVALID_VIN);
    });

    it('меняет value на валидный VIN, затем выполняется поиск по VIN, данные по вин не найдены', async() => {
        const { result } = renderHook(() => useAddExCar());
        const mockFn = jest.fn(() => Promise.reject(VIN_CODE_NOT_FOUND));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockFn);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        await act(async() => {
            await expect(result.current.onSearchCar(VALID_VIN)).rejects.toEqual(VIN_CODE_NOT_FOUND);
        });

        expect(mockFn).toHaveBeenCalledWith(VALID_VIN);

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.NOT_FOUND);
        expect(result.current.data).toEqual(null);
    });

    it('меняет value на валидный VIN, затем выполняется поиск по VIN, неизвестная ошибка', async() => {
        const { result } = renderHook(() => useAddExCar());
        const mockFn = jest.fn(() => Promise.reject(ANY_UNKNOWN_ERROR));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockFn);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        await act(async() => {
            await expect(result.current.onSearchCar(VALID_VIN)).rejects.toEqual(ANY_UNKNOWN_ERROR);
        });

        expect(mockFn).toHaveBeenCalledWith(VALID_VIN);

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.ERROR);
        expect(result.current.data).toEqual(null);
    });

    it('меняет value на валидный VIN, затем выполняется поиска по VIN, неверный вин', async() => {
        const { result } = renderHook(() => useAddExCar());
        const mockFn = jest.fn(() => Promise.reject(VIN_CODE_INVALID));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockFn);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        await act(async() => {
            await expect(result.current.onSearchCar(VALID_VIN)).rejects.toEqual(VIN_CODE_INVALID);
        });

        expect(mockFn).toHaveBeenCalledWith(VALID_VIN);

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.NOT_FOUND);
        expect(result.current.data).toEqual(null);
    });

    it('меняет value на валидный VIN, затем выполняется поиск по VIN, еще нет данных по вин', async() => {
        const { result, waitForNextUpdate } = renderHook(() => useAddExCar());
        const mockRejectedFn = jest.fn(() => Promise.reject(IN_PROGRESS));
        const mockResolvedFn = jest.fn(() => Promise.resolve({ data: { card: '123' } } as GetVehicleInfoResponse));

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementationOnce(mockRejectedFn)
            .mockImplementationOnce(mockResolvedFn);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        await act(async() => {
            await expect(result.current.onSearchCar(VALID_VIN)).rejects.toEqual(IN_PROGRESS);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.LOADING);
        expect(mockRejectedFn).toHaveBeenCalledWith(VALID_VIN);
        expect(mockRejectedFn).toHaveBeenCalledTimes(1);

        act(() => {
            jest.runAllTimers();
        });

        await waitForNextUpdate();

        expect(mockResolvedFn).toHaveBeenCalledTimes(1);

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.SUCCESS);
        expect(result.current.data).toEqual({ data: { card: '123' } });
    });

    it('меняет value на валидный VIN, затем выполняется поиск по VIN, затем машина добавляется в гараж, возвращается card', async() => {
        const { result, waitForNextUpdate } = renderHook(() => useAddExCar());
        const mockGetVehicleInfoByIdentifier = jest.fn(() => Promise.resolve({ data: {} } as GetVehicleInfoResponse));
        const mockAddCardByIdentifier = jest.fn(() => Promise.resolve(CARD as Card));

        (addCardByIdentifier as jest.MockedFunction<typeof addCardByIdentifier>)
            .mockImplementation(mockAddCardByIdentifier);

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockGetVehicleInfoByIdentifier);

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        act(() => {
            result.current.onSearchCar(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.LOADING);
        expect(mockGetVehicleInfoByIdentifier).toHaveBeenCalledWith(VALID_VIN);

        await waitForNextUpdate();

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.SUCCESS);
        expect(result.current.data).toEqual({ data: {} });

        act(() => {
            result.current.onAddCar();
        });

        expect(result.current.addCarStatus).toBe(AddCarStatus.LOADING);
        expect(mockAddCardByIdentifier).toHaveBeenCalledWith({
            identifier: VALID_VIN,
            registration_region_id: undefined,
            card_type: 'EX_CAR',
        });

        await waitForNextUpdate();

        expect(result.current.addCarStatus).toBe(AddCarStatus.LOADING);
        expect(result.current.card).toEqual(CARD);
    });

    it('меняет value на валидный VIN, выполняет поиск по VIN, затем выполняется добавление машины в гараж, возвращается ошибка', async() => {
        const mockGetVehicleInfoByIdentifier = jest.fn(() => Promise.resolve({ data: {} } as GetVehicleInfoResponse));
        const ERROR = 'error';
        const mockAddCardByIdentifier = jest.fn(() => Promise.reject(ERROR));

        (addCardByIdentifier as jest.MockedFunction<typeof addCardByIdentifier>)
            .mockImplementation(mockAddCardByIdentifier);

        (getVehicleInfoByIdentifier as jest.MockedFunction<typeof getVehicleInfoByIdentifier>)
            .mockImplementation(mockGetVehicleInfoByIdentifier);

        const { result, waitForNextUpdate } = renderHook(() => useAddExCar());

        act(() => {
            result.current.onChange(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.INPUT);
        expect(result.current.value).toBe(VALID_VIN);
        expect(result.current.error).toBe('');

        act(() => {
            result.current.onSearchCar(VALID_VIN);
        });

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.LOADING);
        expect(mockGetVehicleInfoByIdentifier).toHaveBeenCalledWith(VALID_VIN);

        await waitForNextUpdate();

        expect(result.current.searchCarStatus).toBe(SearchCarStatus.SUCCESS);
        expect(result.current.data).toEqual({ data: {} });

        expect(result.current.addCarStatus).toBe(AddCarStatus.IDLE);

        await act(async() => {
            await expect(result.current.onAddCar()).rejects.toBe(ERROR);
        });

        expect(result.current.addCarStatus).toBe(AddCarStatus.ERROR);

        expect(mockAddCardByIdentifier).toHaveBeenCalledWith({
            identifier: VALID_VIN,
            registration_region_id: undefined,
            card_type: 'EX_CAR',
        });

        expect(mockDispatch).toHaveBeenCalledTimes(1);
    });
});
