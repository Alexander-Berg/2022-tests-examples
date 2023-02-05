import {tasksResult} from 'mocks/tasks-mock';
import TasksStore from './tasks-store';

function matchStoreWithMock(storeItems: any): void {
    expect(storeItems).toHaveLength(tasksResult.items.length);
    tasksResult.items.forEach((item: any, i) => {
        ['name', 'status'].forEach((prop: any) => {
            expect(item[prop]).toBe(storeItems[i][prop]);
        });
        expect(String(item.id)).toBe(storeItems[i].id);
        ['createTime', 'startTime', 'finishTime'].forEach((prop: any) => {
            expect(new Date(item[prop] * 1000).getTime()).toBe(storeItems[i][prop].getTime());
        });
    });
}

describe('TasksStore', () => {
    let store: TasksStore;

    beforeEach(() => {
        global.fetch.resetMocks();
        store = new TasksStore();
    });

    it('should fetch items', async () => {
        global.fetch.mockResponseOnce(JSON.stringify({data: tasksResult}));

        await store.fetchItems();
        matchStoreWithMock(store.items);
        expect(store.items).toHaveLength(tasksResult.items.length);
    });

    it('should remove item', async () => {
        store.addItems(tasksResult.items);

        matchStoreWithMock(store.items);
        const firstItem = store.items[0];
        await store.removeItem(firstItem);
        expect(store.items).toHaveLength(tasksResult.items.length - 1);
        expect(store.items).not.toContain(firstItem);
    });

    it('should update loading status', () => {
        expect(store.isLoading).toBe(false);
        store.setLoadingStatus();
        expect(store.isLoading).toBe(true);
    });
});
