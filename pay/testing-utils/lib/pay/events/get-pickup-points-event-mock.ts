import { PickupPoint } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

import { generateList } from '../../utils/generate-list';
import { getEventMock } from '../get-event-mock';
import { getPickupPointsMock } from '../get-pickup-points-mock';

import { getUpdateEvent } from './get-update-event';

export function getPickupPointsEventMock(
    pickupPoints: PickupPoint[] = generateList<PickupPoint>(getPickupPointsMock),
) {
    return getEventMock('PickupPoints', faker.datatype.uuid(), getUpdateEvent({ pickupPoints }));
}
