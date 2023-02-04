import { IHouseServiceNotificationBaseProps } from '../../';

export const baseProps: IHouseServiceNotificationBaseProps = {
    title: 'Счёт на оплату выставлен',
    text: 'Собственник выставил вам счёт за коммуналку, вы можете оплатить его.',
};

export const propsWithSpoiler: IHouseServiceNotificationBaseProps = {
    title: 'Счёт на оплату выставлен',
    text: 'Собственник выставил вам счёт за коммуналку, вы можете оплатить его.',
    spoilerTitle: 'Комментарий собственника',
    spoilerContent: 'В этом месяце повышение оплаты за интернет. Провайдер обалдел! Я разберусь в следующем месяце',
};
