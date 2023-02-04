import { ShippingOption } from '@yandex-pay/sdk/src/typings';
import faker from 'faker/locale/ru';

import { generateList } from '../../utils/generate-list';
import { getEventMock } from '../get-event-mock';
import { getShippingOptionMock } from '../get-shipping-option-mock';

import { getUpdateEvent } from './get-update-event';

export function getShippingOptionsEventMock(
    shippingOptions: ShippingOption[] = generateList<ShippingOption>(getShippingOptionMock),
) {
    return getEventMock(
        'ShippingOptions',
        faker.datatype.uuid(),
        getUpdateEvent({ shippingOptions }),
    );
}
