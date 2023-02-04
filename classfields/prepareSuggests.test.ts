import SUGGEST_MOCKS from 'auto-core/react/components/common/ListingResetFiltersSuggest/suggests.mock';

import prepareSuggests from './prepareSuggests';

describe('должен правильно отфильтровать предложения', () => {
    it('обрезать количество предложений, если их больше 5', () => {
        const offersCount = 7;
        const suggests = prepareSuggests(SUGGEST_MOCKS, offersCount);
        expect(suggests).toHaveLength(6);
    });

    it('не отображать Сбросить все, если есть только одно предложение', () => {
        const offersCount = 13;
        const suggests = prepareSuggests(SUGGEST_MOCKS, offersCount);
        expect(suggests).toEqual([
            {
                pagination: {
                    page: 1,
                    page_size: 30,
                    total_page_count: 1,
                    total_offers_count: 15,
                },
                resetKey: 'key1',
                offersCountDelta: 2,
                search_parameters: {},
                suggestText: '',
            },
        ]);
    });

    it('не отображать Сбросить все, если оно не дает прироста', () => {
        const offersCount = 13;
        // проставляем Сбросить все значение меньше текущего значения количества объявлений
        const newSuggestMock = SUGGEST_MOCKS.map(suggest => suggest.resetKey === 'all' ?
            {
                ...suggest,
                pagination: {
                    page: 1,
                    page_size: 30,
                    total_page_count: 1,
                    total_offers_count: 11,
                },
            } : suggest);
        const suggests = prepareSuggests(newSuggestMock, offersCount);
        expect(suggests).toEqual([
            {
                pagination: {
                    page: 1,
                    page_size: 30,
                    total_page_count: 1,
                    total_offers_count: 15,
                },
                resetKey: 'key1',
                offersCountDelta: 2,
                search_parameters: {},
                suggestText: '',
            },
        ]);
    });

    it('не отображать предложения, если они не дают прирост', () => {
        const offersCount = 20;
        const suggests = prepareSuggests(SUGGEST_MOCKS, offersCount);
        expect(suggests).toHaveLength(0);
    });
});
