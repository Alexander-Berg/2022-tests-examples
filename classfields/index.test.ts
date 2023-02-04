import { InsuranceType, InsuranceStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { InsuranceSource } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import getFormattedMoscowDate from 'auto-core/lib/util/time/getFormattedMoscowDate';

import { nbsp } from 'auto-core/react/lib/html-entities';

import dayjs from 'auto-core/dayjs';

import { getInsuranceData, getDaysTo } from './index';

Date.now = jest.fn(() => 1633640055424);

it('getDaysTo возвращает коректное количество дней', () => {
    const nowM10d = dayjs().add(-10, 'd');
    const nowP3d = dayjs().add(3 * 24 + 1, 'h');
    const result = getDaysTo({ from: nowM10d.toISOString(), to: nowP3d.toISOString() });

    expect(result).toBe('3 дня');
});

it('getDaysTo возвращает 5 дней', () => {
    const nowM10d = dayjs().add(-10, 'd');
    const nowP5d = dayjs().add(5 * 24 + 1, 'h');
    const result = getDaysTo({
        from: nowM10d.toISOString(),
        to: nowP5d.toISOString(),
    });

    expect(result).toBe('5 дней');
});

it('getDaysTo возвращает истёк', () => {
    const nowM1D = dayjs().add(-1, 'd');
    const nowM3d = dayjs().add(-365, 'd');
    const result = getDaysTo({ from: nowM3d.toISOString(), to: nowM1D.toISOString() });

    expect(result).toBe('истёк');
});

it('getDaysTo не возвращает 0', () => {
    const now = dayjs();
    const nowM3d = dayjs().add(-365, 'd');
    const result = getDaysTo({ from: nowM3d.toISOString(), to: now.toISOString() });

    expect(result).toBe('1 день');
});

it('getInsuranceData возвращает минимум 2 поля: "Начало действия" и "Серия и номер"', () => {
    const result = getInsuranceData({
        insurance_type: InsuranceType.OSAGO,
        is_actual: true,
        number: '111',
        serial: '',
        status: InsuranceStatus.ACTIVE,
        source: InsuranceSource.UNKNOWN_SOURCE,
    });

    expect(result).toMatchSnapshot();
});

it('getInsuranceData возвращает "Окончание действия"', () => {
    const now = dayjs();
    const nowM1Y = dayjs().add(-365, 'd');
    const nowM1D = dayjs().add(-1, 'd');
    const props: { [key: string]: string | boolean | undefined } = {};

    getInsuranceData({
        serial: '',
        number: '111',
        insurance_type: InsuranceType.OSAGO,
        status: InsuranceStatus.ACTIVE,
        from: nowM1Y.toISOString(),
        is_actual: true,
        source: InsuranceSource.UNKNOWN_SOURCE,
    }).forEach(x => (props[x.key] = x.value));

    expect(props.insuranceTo).toBe(getFormattedMoscowDate(now).replace(/\s/g, nbsp));
    expect(props.serialNumber).toBe('111');

    getInsuranceData({
        serial: 'XXX',
        number: '111',
        insurance_type: InsuranceType.KASKO,
        status: InsuranceStatus.ACTIVE,
        from: nowM1Y.toISOString(),
        to: nowM1D.toISOString(),
        is_actual: true,
        source: InsuranceSource.UNKNOWN_SOURCE,
    }).forEach(x => (props[x.key] = x.value));

    expect(props.insuranceTo).toBe(getFormattedMoscowDate(nowM1D).replace(/\s/g, nbsp));
    expect(props.insuranceFrom).toBe(getFormattedMoscowDate(nowM1Y).replace(/\s/g, nbsp));
    expect(props.serialNumber).toBe('XXX 111');

});
