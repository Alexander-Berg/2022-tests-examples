import { renderHook } from '@testing-library/react-hooks';
import { useDispatch, useSelector } from 'react-redux';

import { ContentSource } from '@vertis/schema-registry/ts-types-snake/auto/lenta/content';

import mockStore from 'autoru-frontend/mocks/mockStore';

import lentaMock from 'auto-core/react/dataDomain/lenta/mocks/lenta';

import { useLenta } from './useLenta';

const mockDispatch = jest.fn();

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

describe('useLenta', () => {
    it('вызовет dispatch с экшеном для смены contentId', async() => {
        mockRedux();
        const { result } = renderHook(() => useLenta());
        const id = lentaMock.items[lentaMock.items.length - 1].id;

        result.current.onFetchMoreClick();

        expect(mockDispatch).toHaveBeenCalledWith({ type: 'LENTA_CHANGE_CONTENT_ID', payload: id });
    });

    it('вызовет dispatch с экшеном для смены source', async() => {
        mockRedux();
        const { result } = renderHook(() => useLenta());

        result.current.onChangeSource(ContentSource.MAGAZINE);

        expect(mockDispatch).toHaveBeenCalledWith({ type: 'LENTA_CHANGE_SOURCE', payload: ContentSource.MAGAZINE });
    });
});

function mockRedux() {
    const store = mockStore({
        lenta: lentaMock,
        garageCard: {
            data: {
                card: {
                    id: '123',
                    user_id: '456',
                },
            },
        },
    });

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (args) => mockDispatch(args),
    );

    return store;
}
