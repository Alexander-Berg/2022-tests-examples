import { getGeoRefinements } from '../index';

interface IMetroListParams {
    id: string;
    name: string;
}

interface IGeoRefinement {
    metro?: {
        name: string;
        list: IMetroListParams[];
    };
}

describe('Корректно отдает данные для метро:', () => {
    it('с МЦК', () => {
        const params: IGeoRefinement = getGeoRefinements({
            params: { metroGeoId: 152941 },
            regionInfo: { ridWithMetro: 213 },
            searchQuery: {},
        });

        expect(params?.metro?.name).toBe('метро МЦК');
    });

    it('с МЦД-1', () => {
        const params: IGeoRefinement = getGeoRefinements({
            params: { metroGeoId: 218597 },
            regionInfo: { ridWithMetro: 213 },
            searchQuery: {},
        });

        expect(params?.metro?.name).toBe('метро МЦД');
    });

    it('с МЦД-2', () => {
        const params: IGeoRefinement = getGeoRefinements({
            params: { metroGeoId: 218549 },
            regionInfo: { ridWithMetro: 213 },
            searchQuery: {},
        });

        expect(params?.metro?.name).toBe('метро МЦД');
    });

    // Тут есть и МЦК и МЦД-1
    it('когда названия станций совпадают с МЦК или МЦД-1', () => {
        const params: IGeoRefinement = getGeoRefinements({
            params: { metroGeoId: 152925 },
            regionInfo: { ridWithMetro: 213 },
            searchQuery: {},
        });

        expect(params?.metro?.name).toBe('метро');
    });

    it('когда названия станций совпадают с МЦД-2', () => {
        const params: IGeoRefinement = getGeoRefinements({
            params: { metroGeoId: 152921 },
            regionInfo: { ridWithMetro: 213 },
            searchQuery: {},
        });

        expect(params?.metro?.name).toBe('метро');
    });
});
