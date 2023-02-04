import { ShippingAddress } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

export function getShippingAddressMock(): ShippingAddress {
    return {
        country: faker.address.country(),
        locality: faker.address.city(),
        street: faker.address.streetAddress(),
        building: faker.address.secondaryAddress(),
    };
}
