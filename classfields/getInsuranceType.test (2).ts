import { InsuranceType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import getInsuranceType from './getInsuranceType';

it('getInsuranceType возвращает КАСКО', () => {
    expect(getInsuranceType(InsuranceType.KASKO)).toEqual('КАСКО');
});

it('getInsuranceType возвращает ОСАГО', () => {
    expect(getInsuranceType(InsuranceType.OSAGO)).toEqual('ОСАГО');
});

it('getInsuranceType возвращает ХЗ', () => {
    expect(getInsuranceType(InsuranceType.UNKNOWN_INSURANCE)).toEqual('Неизвестно');
});
