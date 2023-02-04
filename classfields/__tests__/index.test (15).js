import dayjs from '@realty-front/dayjs';

import { getTariffNotifications } from '../index';
import i18n from '../i18n';

describe('FormGroupTypeError', () => {
    it('Warnings: no_enough_money', () => {
        const tariffData = {
            turnedOn: true,
            warnings: {
                noEnoughMoney: {}
            }
        };

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification.text).toBe(i18n('no_enough_money'));
    });

    it('Warnings: deactivation_announce', () => {
        const tariffData = {
            turnedOn: true,
            warnings: {
                deactivationAnnounce: {
                    deadline: Number(dayjs('2019-10-15'))
                }
            }
        };

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification.text.indexOf('15.10.2019')).not.toBe(-1);
    });

    it('Нет уведомлений', () => {
        const tariffData = {
            turnedOn: true,
            active: {}
        };

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification).toBe(null);
    });

    it('У пользователей с недоступным ТУЗом нет уведомлений', () => {
        const tariffData = {};

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: false,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification).toBe(null);
    });

    it('Показываем только одну причину "NOT_ENOUGH_FUNDS"', () => {
        const tariffData = {
            turnedOn: true,
            inactive: {
                inactiveReason: 'NOT_ENOUGH_FUNDS'
            }
        };

        let notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification.text).toBe(i18n('NOT_ENOUGH_FUNDS'));

        tariffData.inactive.inactiveReason = 'MANUALLY_DISABLED_CAMPAIGN';

        notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification).toBe(null);
    });

    it('Если "NOT_ENOUGH_FUNDS" то не выводим warning про то что скоро закончаться деньги', () => {
        const tariffData = {
            turnedOn: true,
            inactive: {
                inactiveReason: 'NOT_ENOUGH_FUNDS'
            },
            warnings: {
                noEnoughMoney: {}
            }
        };

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: true,
            hasOffers: true
        });

        expect(notification.text).toBe(i18n('NOT_ENOUGH_FUNDS'));
    });

    it('Если данные ТУЗа недоступны - tariff_info_unavailable', () => {
        const tariffData = {};

        const notification = getTariffNotifications({
            tariffData,
            isTariffAvailable: true,
            isTariffLoaded: false,
            hasOffers: true
        });

        expect(notification.text).toBe(i18n('tariff_info_unavailable'));
    });
});
