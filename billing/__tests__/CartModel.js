import CartModel from '../CartModel';

describe('Basic tests of cart\'s view model', () => {
    const mockData = {
        items: [{
            id: 1,
            service_order_id: 1,
            qty: '20',
            service_id: 1,
            act_id: 1,
            payload: {}
        }, {
            id: 2,
            service_order_id: 2,
            qty: '30',
            service_id: 2,
            act_id: 2,
            payload: {}
        }],
        item_count: 2
    };

    const mockCredentials = { service_id: 2, service_order_id: 2 };

    it('Initialises with empty items data', () => {
        const model = new CartModel(()=>{});
        expect(model.data.items).toEqual([]);
        expect(model.data.item_count).toBe(0);
    });

    it('Sets item data', () => {
        const model = new CartModel(()=>{});

        model.data = mockData;
        expect(model.data).toEqual(mockData);
    });

    it('Sets credentials', () => {
        const model = new CartModel(()=>{});

        model.setCredentials(mockCredentials.service_id, mockCredentials.service_order_id);
        expect(model.isCredentialsSet()).toBe(true);
    });

    it('Returns items\' ids', () => {
        const model = new CartModel(()=>{});

        model.data = mockData;
        expect(model.itemIds).toEqual([1, 2]);
    });

    it('Gets items\' count', () => {
        const model = new CartModel(()=>{});

        model.data = mockData;
        expect(model.count).toBe(2);
    });

    it('Returns correct quantity', () => {
        const model = new CartModel(()=>{});

        model.setCredentials(mockCredentials.service_id, mockCredentials.service_order_id);
        model.data = mockData;
        expect(model.quantity).toBe('30');
    });

    it('Calls item data change handler', () => {
        const fn = jest.fn();

        const model = new CartModel(fn);

        model.data = mockData;
        expect(fn).toBeCalledWith(model);
    });
});
