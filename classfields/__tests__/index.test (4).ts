import MockDate from 'mockdate';

import { getDeliveryDates } from '../';

afterAll(() => {
    MockDate.reset();
});

describe('getDeliveryDates', () => {
    it('по дефолту отдает 8 кварталов', () => {
        MockDate.set('2021-02-26T16:00');

        expect(getDeliveryDates()).toMatchSnapshot();
    });

    it('отдает заданное количество кварталов с корректными данными', () => {
        const quartersAmount = 5;
        MockDate.set('2025-12-30T20:30');

        const deliveryDates = getDeliveryDates(quartersAmount);

        expect(deliveryDates).toHaveLength(quartersAmount);
        expect(deliveryDates).toMatchSnapshot();
    });
});
