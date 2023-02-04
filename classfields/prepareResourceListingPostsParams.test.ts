import listingPageMock from 'auto-core/react/dataDomain/mag/listingPage';

import type { AppState } from 'www-mag/react/reducers/AppState';

import prepareResourceListingPostsParams from './prepareResourceListingPostsParams';

let state: AppState;

beforeEach(() => {
    state = {
        mag: {
            listingPage: listingPageMock.value(),
        },
    } as AppState;
});

describe('подготавливает параметры, если', () => {
    it('listingType = category', () => {
        const result = prepareResourceListingPostsParams(state, 1);

        expect(result).toStrictEqual({ categories: [ 'news' ], pageNumber: 1, tags: [ 'bmw-1er' ] });
    });

    it('listingType = tag', () => {
        state.mag.listingPage.listingType = 'tag';
        const result = prepareResourceListingPostsParams(state, 1);

        expect(result).toStrictEqual({ categories: [], pageNumber: 1, tags: [ 'news', 'bmw-1er' ] });
    });

    it('subsection.type = category', () => {
        state.mag.listingPage.subsection = { type: 'category', urlPart: 'games', title: 'Игры' };
        const result = prepareResourceListingPostsParams(state, 1);

        expect(result).toStrictEqual({ categories: [ 'news', 'games' ], pageNumber: 1, tags: [] });
    });

    it('subsection.type = tag', () => {
        state.mag.listingPage.subsection = { type: 'tag', urlPart: 'future', title: 'Будущее' };
        const result = prepareResourceListingPostsParams(state, 1);

        expect(result).toStrictEqual({ categories: [ 'news' ], pageNumber: 1, tags: [ 'future' ] });
    });
});
