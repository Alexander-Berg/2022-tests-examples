import { Order } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

export function getOrderMock(): Order {
    return {
        id: faker.datatype.uuid(),
        total: {
            amount: faker.commerce.price(),
        },
    };
}
