import {sourcesResult} from 'mocks/sources-mock';
import SourcesStore from './sources-store';

function matchStoreWithMock(storeItems: any): void {
    expect(storeItems).toHaveLength(sourcesResult.items.length);
    sourcesResult.items.forEach((source: any, i) => {
        ['sessions', 'id', 'name', 'status'].forEach((prop: any) => {
            expect(source[prop]).toBe(storeItems[i][prop]);
        });
    });
}

describe('SourcesStore', () => {
    let store: SourcesStore;

    beforeEach(() => {
        global.fetch.resetMocks();
        store = new SourcesStore();
    });

    it('should fetch items', async () => {
        global.fetch.mockResponseOnce(JSON.stringify({data: sourcesResult}));

        await store.fetchItems();

        matchStoreWithMock(store.items);
        expect(store.items).toHaveLength(sourcesResult.items.length);
    });

    it('should remove item', async () => {
        store.addItems(sourcesResult.items);

        global.fetch.mockResponseOnce(JSON.stringify({data: {}}));

        matchStoreWithMock(store.items);
        const firstItem = store.items[0];
        await store.removeItem(firstItem);
        expect(store.items).toHaveLength(sourcesResult.items.length - 1);
        expect(store.items).not.toContain(firstItem);
    });

    it('should update loading status', () => {
        expect(store.isLoading).toBe(false);
        store.setLoadingStatus();
        expect(store.isLoading).toBe(true);
    });

    describe('sortBy', () => {
        beforeEach(() => {
            store.addItems(sourcesResult.items);
        });

        it('should sort by sessions(isDesc)', () => {
            store.sortBy('sessions', 'desc');
            [35, 34, 33, 32, 31, 30].forEach((count, i) => {
                expect(store.items[i].sessions).toBe(count);
            });
            expect(store.orderState).toEqual({orderBy: 'sessions', order: 'desc'});
        });

        it('should sort by id', () => {
            store.sortBy('id', 'asc');
            [
                'neq/2018-09-11/moskva',
                'neq/2018-09-12/spb',
                'neq/2018-09-13/minsk',
                'neq/2018-09-14/komm_2018Q3',
                'neq/2018-09-14/komm_2019Q3',
                'neq/2018-09-14/komm_2020Q3'

            ].forEach((item, i) => {
                expect(store.items[i].id).toBe(item);
            });
            expect(store.orderState).toEqual({orderBy: 'id', order: 'asc'});
        });

        it('should sort by id(Desc)', () => {
            store.sortBy('id', 'desc');
            [
                'neq/2018-09-11/moskva',
                'neq/2018-09-12/spb',
                'neq/2018-09-13/minsk',
                'neq/2018-09-14/komm_2018Q3',
                'neq/2018-09-14/komm_2019Q3',
                'neq/2018-09-14/komm_2020Q3'

            ].reverse().forEach((item, i) => {
                expect(store.items[i].id).toBe(item);
            });
            expect(store.orderState).toEqual({orderBy: 'id', order: 'desc'});
        });

        it('should sort by modificationTime', () => {
            store.sortBy('modificationTime', 'asc');
            [
                1538504213,
                1538590613,
                1538677013,
                1538763413,
                1538849813,
                1538936213

            ].forEach((item, i) => {
                expect(store.items[i].modificationTime).toEqual(new Date(item * 1000));
            });
            expect(store.orderState).toEqual({orderBy: 'modificationTime', order: 'asc'});
        });

        it('should sort by modificationTime(isDesc)', () => {
            store.sortBy('modificationTime', 'desc');
            [
                1538504213,
                1538590613,
                1538677013,
                1538763413,
                1538849813,
                1538936213

            ].reverse().forEach((item, i) => {
                expect(store.items[i].modificationTime).toEqual(new Date(item * 1000));
            });
            expect(store.orderState).toEqual({orderBy: 'modificationTime', order: 'desc'});
        });
    });
});
