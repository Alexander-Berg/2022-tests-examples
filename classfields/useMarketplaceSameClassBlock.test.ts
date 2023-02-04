jest.mock('auto-core/react/dataDomain/relatedGroups/actions/fetchRelatedGroups');
jest.mock('auto-core/react/dataDomain/compare/actions/add');

import { renderHook, act } from '@testing-library/react-hooks';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import fetchRelatedGroups from 'auto-core/react/dataDomain/relatedGroups/actions/fetchRelatedGroups';
import add from 'auto-core/react/dataDomain/compare/actions/add';

import type TContext from 'auto-core/types/TContext';

import state from '../mocks/state.mock';

import useMarketplaceSameClassBlock from './useMarketplaceSameClassBlock';

const store = mockStore(state);

const fetchRelatedGroupsMock = fetchRelatedGroups as jest.MockedFunction<typeof fetchRelatedGroups>;
const addMock = add as jest.MockedFunction<typeof add>;

beforeAll(() => {
    jest.useFakeTimers();
});

afterAll(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
});

it('при изменении цены до обновляет relatedGroup и меняет цену после промиса и отправляет метрику', async() => {
    expect.assertions(4);

    fetchRelatedGroupsMock.mockReturnValue(() => new Promise(resolve => setTimeout(resolve, 100)));

    const { result, waitForValueToChange } = render();

    act(() => {
        result.current.changePriceTo([ 200000 ]);
    });

    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith(
        [ 'marketplace-model', 'blocks', 'same-class', 'price-to', '200000', 'click' ],
    );

    expect(fetchRelatedGroupsMock).toHaveBeenCalled();
    expect(result.current.currentPrice).toEqual(0);

    act(() => {
        jest.runAllTimers();
    });

    await waitForValueToChange(() => result.current.currentPrice);

    expect(result.current.currentPrice).toEqual(200000);
});

it('правильно отрабатывает добавление моделей в сравнение', async() => {
    addMock.mockReturnValue(() => new Promise(resolve => setTimeout(resolve, 100)));

    const { result, waitForValueToChange } = render();

    act(() => {
        result.current.addModelsToCompare();
    });

    expect(result.current.isComparePending).toBe(true);
    expect(addMock).toHaveBeenCalledTimes(9);

    act(() => {
        jest.runAllTimers();
    });

    await waitForValueToChange(() => result.current.isComparePending);

    expect(result.current.isComparePending).toBe(false);
});

function render() {
    const pageParams = {
        mark: 'renault',
        model: 'logan',
        super_gen: '1',
        configuration_id: '1',
    };
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);
    mockUseDispatch(store);
    return renderHook(() => useMarketplaceSameClassBlock({ ...contextMock, pageParams } as unknown as TContext));
}
