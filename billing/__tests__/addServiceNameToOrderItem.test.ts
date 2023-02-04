import { fromJS } from 'immutable';

import { addServiceNameToOrderItem } from '../ListContainer';

describe('addServiceNameToOrderItem', () => {
    it('should add service name to item', () => {
        const services = fromJS([
            {
                val: 'cc_1',
                text: 'CC 1'
            },
            {
                val: 'cc_2',
                text: 'CC 2'
            },
            {
                val: 'cc_3',
                text: 'CC 3'
            }
        ]);

        const items = fromJS([{ serviceCc: 'cc_1' }, { serviceCc: 'cc_2' }, { serviceCc: 'cc_3' }]);

        const jsItems = addServiceNameToOrderItem(services, items);

        expect(jsItems[0].serviceName).toBe('CC 1');
        expect(jsItems[1].serviceName).toBe('CC 2');
        expect(jsItems[2].serviceName).toBe('CC 3');
    });

    it('should not fail if services is null', () => {
        const items = fromJS([{ serviceCc: 'cc_1' }, { serviceCc: 'cc_2' }, { serviceCc: 'cc_3' }]);

        const jsItems = addServiceNameToOrderItem(null, items);

        expect(jsItems[0].serviceName).toBeNull();
        expect(jsItems[1].serviceName).toBeNull();
        expect(jsItems[2].serviceName).toBeNull();
    });

    it('should not fail if there are no items', () => {
        const services = fromJS([
            {
                val: 'cc_1',
                text: 'CC 1'
            },
            {
                val: 'cc_2',
                text: 'CC 2'
            },
            {
                val: 'cc_3',
                text: 'CC 3'
            }
        ]);

        addServiceNameToOrderItem(services, fromJS([]));
    });

    it('should not fail if service could not find by cc', () => {
        const services = fromJS([
            {
                val: 'cc_1',
                text: 'CC 1'
            },
            {
                val: 'cc_2',
                text: 'CC 2'
            },
            {
                val: 'cc_3',
                text: 'CC 3'
            }
        ]);

        const items = fromJS([{ serviceCc: 'cc_1' }, { serviceCc: 'cc_2' }, { serviceCc: 'cc_4' }]);

        const jsItems = addServiceNameToOrderItem(services, items);

        expect(jsItems[0].serviceName).toBe('CC 1');
        expect(jsItems[1].serviceName).toBe('CC 2');
        expect(jsItems[2].serviceName).toBeNull();
    });
});
