import { parseServiceList } from '../completions';

const serviceList = [
    { id: 1, name: 'service_1', hasAggregation: true },
    { id: 2, name: 'service_2', hasAggregation: true },
    { id: 3, name: 'service_3', hasAggregation: false },
    { id: 4, name: 'service_4', hasAggregation: true },
    { id: 5, name: 'service_5', hasAggregation: false },
    { id: 6, name: 'service_6', hasAggregation: false }
];
describe('Test completions data-processor methods', () => {
    test('parseServiceList', () => {
        const expected = {
            serviceItems: [
                { value: 1, content: 'service_1' },
                { value: 2, content: 'service_2' },
                { value: 3, content: 'service_3' },
                { value: 4, content: 'service_4' },
                { value: 5, content: 'service_5' },
                { value: 6, content: 'service_6' }
            ],
            serviceMap: {
                1: { id: 1, name: 'service_1', hasAggregation: true },
                2: { id: 2, name: 'service_2', hasAggregation: true },
                3: { id: 3, name: 'service_3', hasAggregation: false },
                4: { id: 4, name: 'service_4', hasAggregation: true },
                5: { id: 5, name: 'service_5', hasAggregation: false },
                6: { id: 6, name: 'service_6', hasAggregation: false }
            }
        };

        expect(parseServiceList(serviceList)).toEqual(expected);
    });
});
