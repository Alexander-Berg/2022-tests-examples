import { InsuranceType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import getInsuranceType from './getInsuranceType';

it('должен вернуть ОСАГО', () => {
    const result = getInsuranceType(InsuranceType.OSAGO);
    expect(result).toEqual('ОСАГО');
});

it('должен вернуть КАСКО', () => {
    const result = getInsuranceType(InsuranceType.KASKO);
    expect(result).toEqual('КАСКО');
});

it('должен вернуть "Другой"', () => {
    const result = getInsuranceType(InsuranceType.UNKNOWN_INSURANCE);
    expect(result).toEqual('Другой');
});
