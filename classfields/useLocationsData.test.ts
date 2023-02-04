/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

import { useDispatch } from 'react-redux';
import { renderHook } from '@testing-library/react-hooks';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';

import { c2bAuctionDefaultPlace } from '../../hooks/useC2bAuction';

import { useLocationsData } from './useLocationsData';

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

const getResourceMock = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const handleChangeDateMock = jest.fn();
const handleChangeTimeMock = jest.fn();

const initialParams = {
    date: [] as Array<string>,
    place: c2bAuctionDefaultPlace,
    time: '',
    handleChangeDate: handleChangeDateMock,
    handleChangeTime: handleChangeTimeMock,
};

describe('useLocationsData', () => {

    createStore();

    beforeEach(() => {
        handleChangeDateMock.mockClear();
        handleChangeTimeMock.mockClear();
        getResourceMock.mockClear();
    });

    it('значения по умолчанию', () => {
        getResourceMock.mockResolvedValue({});

        const { result } = renderHook((params) => useLocationsData(params), {
            initialProps: {
                ...initialParams,
            },
        });

        expect(result.current.locationList).toEqual([]);
        expect(result.current.datesList).toEqual([]);
        expect(result.current.timesList).toEqual([]);
        expect(result.current.remoteLocationId).toBe('');
    });

    it('при маунте запрашивает и сохраняет список локаций', async() => {
        getResourceMock.mockResolvedValue({
            officeLocations: [ { id: '1', address: 'moscow' } ],
            remoteLocation: { id: '123', address: 'remote' },
        });

        const { result } = renderHook((params) => useLocationsData(params), {
            initialProps: {
                ...initialParams,
            },
        });

        await flushPromises();

        expect(result.current.locationList).toEqual([ { id: '1', address: 'moscow' } ]);
        expect(result.current.remoteLocationId).toBe('123');
    });

    it('при смене локации сбрасывает дату и время, и запрашивает новый список дат', async() => {
        getResourceMock.mockResolvedValue({
            officeLocations: [ { id: '1', address: 'moscow' } ],
            remoteLocation: { id: '123', address: 'remote' },
        });

        const { rerender, result } = renderHook((params) => useLocationsData(params), {
            initialProps: {
                ...initialParams,
            },
        });

        getResourceMock.mockResolvedValue({
            dates: [ '12-11-2021', '13-11-2021' ],
        });

        //поменялась локация
        rerender({
            ...initialParams,
            place: { address: 'moscow', lat: 20, lon: 30 },
        });

        await flushPromises();

        expect(handleChangeDateMock).toHaveBeenCalledWith([]);
        expect(handleChangeTimeMock).toHaveBeenCalledWith('');
        expect(result.current.datesList).toEqual([ '12-11-2021', '13-11-2021' ]);
    });

    it('при смене даты сбрасывает время и запрашивает новый список времени осмотра', async() => {
        getResourceMock.mockResolvedValue({
            officeLocations: [ { id: '1', address: 'moscow' } ],
            remoteLocation: { id: '123', address: 'remote' },
        });

        const { rerender, result } = renderHook((params) => useLocationsData(params), {
            initialProps: {
                ...initialParams,
            },
        });

        getResourceMock.mockResolvedValue({
            times: [ '12:00', '13:00' ],
        });

        //поменялась дата
        rerender({
            ...initialParams,
            date: [ '12-11-2023' ],
        });

        await flushPromises();

        expect(handleChangeDateMock).toHaveBeenCalledTimes(0);
        expect(handleChangeTimeMock).toHaveBeenCalledWith('');
        expect(result.current.timesList).toEqual([ '12:00', '13:00' ]);
    });
});

function createStore() {
    const store = mockStore();

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );
}
