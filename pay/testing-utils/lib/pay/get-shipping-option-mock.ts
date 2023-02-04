import { ShippingOption } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

export function getShippingOptionMock(): ShippingOption {
    return {
        id: faker.datatype.uuid(),
        provider: 'DHL',
        amount: faker.commerce.price(),
    };
}
