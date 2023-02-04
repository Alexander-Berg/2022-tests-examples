import { UpdatePaymentData } from '@yandex-pay/sdk/src/typings';

export function getUpdateEvent(updateData: UpdatePaymentData): UpdatePaymentData {
    return { updateData };
}
