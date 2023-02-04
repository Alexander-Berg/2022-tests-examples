jest.mock('auto-core/react/dataDomain/compare/actions/add');
jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

import { useDispatch } from 'react-redux';
import { renderHook, act } from '@testing-library/react-hooks';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';
import compareMock from 'autoru-frontend/mockData/state/compare.mock';

import modelMock from 'www-mobile/react/components/PageMarketplaceMark/MarketplaceModelCard/mocks/model.mock';

import useMarketplaceCompare from './useMarketplaceCompare';

import '@testing-library/jest-dom';

const defaultState = {
    compare: compareMock,
};

const defaultProps = { offer: modelMock };

beforeAll(() => {
    jest.useFakeTimers();
});

afterAll(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
});

it('правильно отрабатывает клик по добавлению в сравнение', async() => {
    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockImplementation(
        () => jest.fn().mockReturnValue(new Promise(resolve => setTimeout(resolve, 100))),
    );

    const { result, waitFor } = render({});

    act(() => {
        result.current.addOfferToCompare(null, { data: '1_1_1' });
    });
    await waitFor(() => result.current.isComparePending);

    act(() => {
        jest.runAllTimers();
    });
    await waitFor(() => !result.current.isComparePending);
    expect(result.current.isComparePending).toBe(false);
});

function render({ state = defaultState, props = defaultProps }) {
    const { mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);

    return renderHook(() => useMarketplaceCompare(props));
}
