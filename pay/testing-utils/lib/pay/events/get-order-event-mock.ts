import { Order } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

import { getEventMock } from '../get-event-mock';
import { getOrderMock } from '../get-order-mock';

import { getUpdateEvent } from './get-update-event';

export function getOrderEventMock(order: Order = getOrderMock()) {
    return getEventMock('Order', faker.datatype.uuid(), getUpdateEvent({ order }));
}
