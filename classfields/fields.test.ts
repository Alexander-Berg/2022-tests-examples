import { InsuranceType, InsuranceStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import type { Insurance as TInsurance } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { InsuranceSource } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';

import { getErrors } from 'auto-core/react/components/common/GarageCardInsuranceList/helpers/form';
import type { TSetter } from 'auto-core/react/components/common/GarageCardInsuranceList/helpers/form';

import type {
    InsuranceFormFields } from './fields';
import {
    currentYear,
    devideNumber,
    fields,
    toUpper,
} from './fields';

const runCompose = <T, TValues>(list: TSetter<T, TValues> | Array<TSetter<T, TValues>>, args: [ string | Array<string>, T, keyof TValues]) => {
    if (!list) {
        return;
    }
    const all = Array.isArray(list) ? list : [ list ];

    all.forEach(x => x(...args));
};

it('toUpper преобразует в том числе латиницу в кириллицу', () => {
    const translit = 'хтснрмевакХТСНРМЕВАК'.split('').map(toUpper).join('');

    expect(translit).toBe('XTCHPMEBAKXTCHPMEBAK');
});

it('devideNumber делит на серию и номер строку по пробелу', () => {
    const osago = devideNumber('XXX 1234567890', { insurance_type: InsuranceType.OSAGO });

    expect(osago).toEqual({ serial: 'XXX', number: '1234567890' });

    const kasko = devideNumber('1234567890', { insurance_type: InsuranceType.KASKO });

    expect(kasko).toEqual({ serial: '', number: '1234567890' });
});

it('InsuranceFormFields сеттеры должны работать для каско', () => {
    const data: InsuranceFormFields = {
        from: 'from',
        insurance_type: [ InsuranceType.KASKO ] as unknown as InsuranceType,
        name: 'name',
        number: 'number',
        phone_number: 'phone_number',
        serial: 'serial',
        to: 'to',
    };
    const obj: TInsurance = {
        company: {
            name: 'name',
            phone_number: 'phone_number',
        },
        from: 'from',
        insurance_type: InsuranceType.KASKO,
        number: 'number',
        serial: '',
        to: 'to',
        is_actual: true,
        status: InsuranceStatus.UNKNOWN_STATUS,
        source: InsuranceSource.UNKNOWN_SOURCE,
    };
    const insurance: TInsurance = {
        serial: '',
        number: '',
        insurance_type: InsuranceType.OSAGO,
        status: InsuranceStatus.UNKNOWN_STATUS,
        is_actual: true,
        source: InsuranceSource.UNKNOWN_SOURCE,
    };
    fields.forEach(field => {
        field.setter && runCompose<TInsurance, InsuranceFormFields>(field.setter, [ data[field.name] as string, insurance, field.name ]);
    });

    expect(insurance).toEqual(obj);

    const errors = getErrors(fields, obj);
    expect(errors).toEqual({ to: 'Проверьте пожалуйста значения', from: 'Проверьте пожалуйста значения' });
});

it('InsuranceFormFields сеттеры должны работать для осаго', () => {
    const data: InsuranceFormFields = {
        from: '25.09.' + currentYear,
        insurance_type: [ InsuranceType.OSAGO ] as unknown as InsuranceType,
        name: 'name',
        number: 'XXX 1234567890',
        phone_number: 'phone_number',
        serial: '',
    };
    const obj: TInsurance = {
        company: {
            name: 'name',
            phone_number: 'phone_number',
        },
        from: '25.09.' + currentYear,
        insurance_type: InsuranceType.OSAGO,
        number: '1234567890',
        serial: 'XXX',
        to: '25.09.' + (currentYear + 1),
        is_actual: true,
        status: InsuranceStatus.UNKNOWN_STATUS,
        source: InsuranceSource.UNKNOWN_SOURCE,
    };
    const insurance: TInsurance = {
        serial: '',
        number: '',
        insurance_type: InsuranceType.KASKO,
        status: InsuranceStatus.UNKNOWN_STATUS,
        is_actual: true,
        source: InsuranceSource.UNKNOWN_SOURCE,
    };
    fields.forEach(field => {
        field.setter && field.name in data && runCompose<TInsurance, InsuranceFormFields>(field.setter, [ data[field.name] as string, insurance, field.name ]);
    });

    expect(insurance).toEqual(obj);

    insurance.from = '25.09.1981';
    insurance.to = '13.09.1982';

    const errors1 = getErrors(fields, insurance);
    expect(errors1).toEqual({ to: 'Проверьте пожалуйста значения', from: 'Проверьте пожалуйста значения' });

    insurance.from = '25.09.' + (currentYear + 9);
    insurance.to = '13.09.' + (currentYear + 10);

    const errors2 = getErrors(fields, insurance);
    expect(errors2).toEqual({ to: 'Проверьте пожалуйста значения', from: 'Проверьте пожалуйста значения' });

    insurance.from = '';
    insurance.to = '25.09.' + currentYear;

    const errors3 = getErrors(fields, insurance);
    expect(errors3).toEqual({ from: 'Нужно заполнить это поле' });

    insurance.from = '25.09.' + currentYear;
    insurance.to = '13.09.' + (currentYear + 10);

    const errors4 = getErrors(fields, insurance);
    expect(errors4).toEqual({ to: 'Срок должен быть от 5 до 365 дней' });
});
