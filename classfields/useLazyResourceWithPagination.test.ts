jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn().mockResolvedValue({}),
    };
});

import { renderHook, act } from '@testing-library/react-hooks';

import gateApi from 'auto-core/react/lib/gateApi';

import useLazyResourceWithPagination from './useLazyResourceWithPagination';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const getters = {
    getTotalPages: jest.fn(),
    getDataArray: jest.fn(),
};

it('должен запросить данные по init()', () => {
    const { result } = render();
    act(() => {
        result.current.init();
    });
    expect(getResource).toHaveBeenCalledWith('resourceName', { page: 1, page_size: 8 });
});

describe('hasNextPage', () => {
    it('возвращает false, если страниц больше нет', async() => {
        getters.getTotalPages.mockReturnValue(1);
        getters.getDataArray.mockReturnValue([]);
        const { result, waitFor } = render();
        act(() => {
            result.current.init();
        });
        await waitFor(() => !result.current.hasNextPage);

        expect(result.current.hasNextPage).toEqual(false);
    });
    it('возвращает true, если еще есть страницы', async() => {
        getters.getTotalPages.mockReturnValue(100);
        getters.getDataArray.mockReturnValue([]);
        const { result, waitFor } = render();
        act(() => {
            result.current.init();
        });
        await waitFor(() => result.current.hasNextPage);

        expect(result.current.hasNextPage).toEqual(true);
    });
});

it('должен получить данные по loadNextPage и добавить их в data', async() => {
    const mock = {
        food: [ 'pelmeni', 'borsch' ],
        someBullshit: 'нъне',
    };
    getters.getTotalPages.mockReturnValue(2);
    getters.getDataArray.mockReturnValue(mock.food);
    getResource.mockResolvedValue(mock);
    const { result, waitFor } = render();
    act(() => {
        result.current.init();
    });
    await waitFor(() => result.current.hasNextPage);

    expect(result.current.hasNextPage).toEqual(true);
    expect(result.current.isError).toEqual(false);
    expect(result.current.isLoading).toEqual(false);
    expect(result.current.origResponse).toEqual(mock);
    expect(result.current.data).toEqual(mock.food);

    act(() => {
        result.current.loadNextPage();
    });
    await waitFor(() => result.current.hasNextPage);

    expect(result.current.hasNextPage).toEqual(false);
    expect(result.current.isError).toEqual(false);
    expect(result.current.isLoading).toEqual(false);
    expect(result.current.origResponse).toEqual(mock);
    expect(result.current.data).toEqual([ ...mock.food, ...mock.food ]);
});

it('правильно отработает реджект промиса', async() => {
    getResource.mockRejectedValue({});
    const { result, waitFor } = render();
    act(() => {
        result.current.init();
    });
    await waitFor(() => result.current.isError);
    expect(result.current.isError).toEqual(true);
    expect(result.current.isLoading).toEqual(false);
});

function render() {
    return renderHook(() => useLazyResourceWithPagination<unknown, unknown>({
        resourceName: 'resourceName',
        resourceParams: {},
        ...getters,
    }));
}
