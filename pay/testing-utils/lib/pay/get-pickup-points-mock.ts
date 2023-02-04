import { PickupPoint } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

export function getPickupPointsMock(): PickupPoint {
    return {
        id: faker.datatype.uuid(),
        label: faker.company.companyName(),
        address: faker.address.streetAddress(true),
        timeFrom: faker.date.soon(3).getTime(),
        amount: faker.commerce.price(),
        coordinates: {
            latitude: faker.datatype.float(7),
            longitude: faker.datatype.float(7),
        },
    };
}
