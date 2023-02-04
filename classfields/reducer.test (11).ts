import _ from 'lodash';

import { ContentSource } from '@vertis/schema-registry/ts-types-snake/auto/lenta/content';

import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import ActionTypes from 'auto-core/react/dataDomain/lenta/actionTypes';
import lentaMock from 'auto-core/react/dataDomain/lenta/mocks/lenta';

import reducer from './reducer';

const STATE = {
    totalItemsCount: lentaMock.pageStatistics.totalCount,
    source: lentaMock.source,
    items: lentaMock.items,
    pageStatistics: lentaMock.pageStatistics,
    isFetching: false,
    isError: false,
};

describe('Lenta reducer', () => {
    it('вернет состояние после успешной загрузки страницы', () => {
        const actual = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {
            lenta: {
                data: {
                    items: lentaMock.items,
                    pageStatistics: lentaMock.pageStatistics,
                    source: lentaMock.source,
                },
            },
        } });

        expect(actual).toEqual(STATE);
    });

    it('вернет состояние загрузки данных', () => {
        const actual = reducer(STATE, { type: ActionTypes.LENTA_FETCHING });

        expect(actual).toEqual({
            ...STATE,
            isFetching: true,
            isError: false,
        });
    });

    it('вернет состояние после успешной загрузки данных', () => {
        const newItems = _.cloneDeep(lentaMock.items);
        newItems[0].id = '1';
        newItems[1].id = '2';
        newItems[2].id = '3';

        const nextPageStatistics = {
            totalCount: lentaMock.pageStatistics.totalCount,
            shownContentCount: lentaMock.pageStatistics.shownContentCount,
            fromContentId: '1',
        };

        const actual = reducer(STATE, { type: ActionTypes.LENTA_RESOLVED, payload: {
            data: {
                items: newItems,
                pageStatistics: nextPageStatistics,
                source: ContentSource.ALL,
            },
        } });

        expect(actual).toEqual({
            totalItemsCount: lentaMock.pageStatistics.totalCount,
            items: newItems,
            pageStatistics: nextPageStatistics,
            source: ContentSource.ALL,
            isFetching: false,
            isError: false,
        });
    });

    it('вернет состояние после успешной загрузки данных следующей "страницы"', () => {
        const state = _.cloneDeep(STATE);
        // меняем contentId на id последнего объекта,
        // в этом состоянии выполняется загрузка новых данных

        const lastItem = _.cloneDeep(lentaMock.items[lentaMock.items.length - 1]);
        state.pageStatistics.fromContentId = lastItem.id;

        const newItems = _.cloneDeep(lentaMock.items);
        newItems[0].id = '1';
        newItems[1].id = '2';
        newItems[2].id = '3';

        const resultItems = [ ...state.items, ...newItems ];

        const nextPageStatistics = {
            totalCount: lentaMock.pageStatistics.totalCount,
            shownContentCount: lentaMock.pageStatistics.shownContentCount,
            fromContentId: lastItem.id,
        };

        const actual = reducer(state, { type: ActionTypes.LENTA_FETCH_MORE_RESOLVED, payload: {
            data: {
                items: newItems,
                pageStatistics: nextPageStatistics,
                source: ContentSource.ALL,
            },
        } });

        expect(actual).toEqual({
            totalItemsCount: lentaMock.pageStatistics.totalCount,
            items: resultItems,
            pageStatistics: nextPageStatistics,
            source: ContentSource.ALL,
            isFetching: false,
            isError: false,
        });
    });

    it('вернет состояние после ошибки загрузки данных', () => {
        const actual = reducer(STATE, { type: ActionTypes.LENTA_REJECTED });

        expect(actual).toEqual({
            ...STATE,
            isError: true,
        });
    });

    it('вернет состояние после изменения source', () => {
        const actual = reducer(STATE, { type: ActionTypes.LENTA_CHANGE_SOURCE, payload: ContentSource.MAGAZINE });

        expect(actual).toEqual({
            ...STATE,
            source: ContentSource.MAGAZINE,
            pageStatistics: {
                ...STATE.pageStatistics,
                fromContentId: undefined,
            },
        });
    });

    it('вернет состояние после изменения fromContentId', () => {
        const ID = '1';
        const actual = reducer(STATE, { type: ActionTypes.LENTA_CHANGE_CONTENT_ID, payload: ID });

        expect(actual).toEqual({
            ...STATE,
            pageStatistics: {
                ...STATE.pageStatistics,
                fromContentId: ID,
            },
        });
    });
});
