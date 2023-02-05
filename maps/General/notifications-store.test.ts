import {notifications} from 'mocks/notifications-mock';
import NotificationsStore from './notifications-store';

function matchStoreWithMock(storeItems: any): void {
    expect(storeItems).toHaveLength(notifications.length);
    const idsList = new Set<number>();
    notifications.forEach((item: any, i) => {
        ['type', 'title', 'message'].forEach((prop: any) => {
            expect(item[prop]).toBe(storeItems[i][prop]);
            idsList.add(storeItems[i].id);
        });
    });
    expect(storeItems.length).toBe(idsList.size);
}

describe('NotificationsStore', () => {
    let store: NotificationsStore;

    beforeEach(() => {
        store = new NotificationsStore();
    });

    it('should add item', () => {
        notifications.forEach((item) => store.add(item));
        matchStoreWithMock(store.items);
    });

    it('should remove item', () => {
        notifications.forEach((item) => store.add(item));
        matchStoreWithMock(store.items);
        const firstItem = store.items[0];
        store.remove(firstItem);
        expect(store.items).toHaveLength(notifications.length - 1);
        expect(store.items).not.toContain(firstItem);
    });
});
