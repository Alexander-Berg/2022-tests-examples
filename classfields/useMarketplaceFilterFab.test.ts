import { renderHook, act } from '@testing-library/react-hooks';
import { fireEvent } from '@testing-library/react';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import type TContext from 'auto-core/types/TContext';

import useMarketplaceFilterFab from './useMarketplaceFilterFab';

const tagsDictionary = [
    {
        name: 'ТЭГ1',
        code: 'tag1',
    },
    {
        name: 'ТЭГ2',
        code: 'tag2',
    },
    {
        name: 'ТЭГ3',
        code: 'tag3',
    },
];

const state = {
    listing: {
        data: {
            search_parameters: {
                section: 'new',
                category: 'cars',
                catalog_filter: [ {
                    mark: 'AUDI',
                    model: 'Q7',
                    generation: '21646875',
                    configuration: '21646934',
                } ],
            },
        },
    },
    searchTagDictionary: tagsDictionary,
};

const store = mockStore(state);

it('правильно отрабатывает появление/скрытие фаба', async() => {
    const { result } = render();

    expect(result.current.isFabVisible).toBe(true);

    act(() => {
        fireEvent.scroll(window, { target: { scrollY: 100 } });
    });
    expect(result.current.isFabVisible).toBe(false);

    act(() => {
        fireEvent.scroll(window, { target: { scrollY: 0 } });
    });
    expect(result.current.isFabVisible).toBe(true);
});

it('правильно отрабатывает открытие/закрытие фильтра', () => {
    const { result } = render();

    expect(result.current.isFiltersVisible).toBe(false);

    act(() => {
        result.current.showFiltersPopup();
    });
    expect(result.current.isFiltersVisible).toBe(true);
    act(() => {
        result.current.closeFiltersPopup();
    });
    expect(result.current.isFiltersVisible).toBe(false);
});

it('правильно отработает смену параметров в фильтре', () => {
    const { result } = render();

    result.current.onFiltersSubmit({
        section: 'all',
        category: 'cars',
        catalog_filter: [ {
            mark: 'AUDI',
            model: 'Q7',
        } ],
    });

    expect(contextMock.pushState).toHaveBeenCalledWith(
        'link/listing/?section=all&category=cars&catalog_filter=mark%3DAUDI%2Cmodel%3DQ7',
        { loadData: true },
    );
});

function render() {
    const { mockUseDispatch, mockUseSelector } = applyUseSelectorMock();
    mockUseSelector(state);
    mockUseDispatch(store);
    return renderHook(() => useMarketplaceFilterFab(contextMock as unknown as TContext));
}
