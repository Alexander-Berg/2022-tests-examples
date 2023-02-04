import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';
import { CategoryItems, SectionItems } from 'auto-core/types/TSearchParameters';

import { getFooterTitle } from './getFooterTitle';

it('отдаст тайтл для марки', () => {
    const searchParams = {
        category: CategoryItems.CARS,
        section: SectionItems.ALL,
        catalog_filter: [
            { mark: 'FORD' },
        ],
    };

    expect(getFooterTitle({ firstOffer: offerMock, searchParams }))
        .toBe('Все Ford на Авто.ру');
});

it('отдаст тайтл для легковых', () => {
    const searchParams = {
        category: CategoryItems.CARS,
        section: SectionItems.ALL,
    };

    expect(getFooterTitle({ firstOffer: offerMock, searchParams }))
        .toBe('Все автомобили на Авто.ру');
});

it('отдаст тайтл для мото', () => {
    const searchParams = {
        category: CategoryItems.MOTO,
        section: SectionItems.ALL,
        moto_category: 'MOTORCYCLE',
    } as TSearchParameters;

    expect(getFooterTitle({ firstOffer: offerMock, searchParams }))
        .toBe('Все мотоциклы на Авто.ру');
});

it('отдаст тайтл для lcv', () => {
    const searchParams = {
        category: CategoryItems.TRUCKS,
        section: SectionItems.ALL,
        trucks_category: 'BULLDOZERS',
    } as TSearchParameters;

    expect(getFooterTitle({ firstOffer: offerMock, searchParams }))
        .toBe('Все бульдозеры на Авто.ру');
});
