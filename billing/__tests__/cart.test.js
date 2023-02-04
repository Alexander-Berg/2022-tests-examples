import cloneDeep from 'lodash.clonedeep';

import { ItemType } from '../../types';
import cart, { List } from '../cart';
import { CART } from '../../actions';

const response = {
    data: {
        item_count: 2,
        items: [
            {
                client_id: 106591190,
                dt: '2019-04-04T12:57:17',
                id: 219,
                order_id: 1100739386,
                qty: '1',
                service_id: 11,
                service_order_id: 10589901006,
                title: 'Один',
                units_name: 'штук'
            },
            {
                client_id: 106591190,
                dt: '2019-04-04T12:57:25',
                id: 239,
                order_id: 1100739387,
                qty: '2',
                service_id: 11,
                service_order_id: 10589901007,
                title: 'Два',
                units_name: 'штук'
            }
        ]
    },
    version: { butils: '2.130', muzzle: 'UNKNOWN', snout: '1.0.183' }
};

describe('user - cart - reducers - cart', () => {
    it('delete items - should correctly merge current and received data', () => {
        expect.assertions(5);

        let state = List();

        state = cart(state, {
            type: CART.RECEIVE,
            items: response.data.items,
            invoices: []
        });

        expect(state.items.size).toBe(2);

        state = cart(state, {
            type: CART.ACTIVE_CHANGE,
            id: 219,
            itemType: ItemType.Order,
            active: false
        });

        expect(state.items.find(i => i.id === 219).active).toBeFalsy();

        const item = cloneDeep(response.data.items[0]);
        item.title = undefined;
        item.units_name = undefined;

        state = cart(state, { type: CART.DELETE_ITEM_SUCCESS, items: [item] });

        expect(state.items.find(i => i.id === 219).title).toBe('Один');
        expect(state.items.find(i => i.id === 219).active).toBeFalsy();
        expect(state.items.size).toBe(1);
    });
});
