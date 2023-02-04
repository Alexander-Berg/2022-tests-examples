import { InsuranceStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import getInsuranceStatus from './getInsuranceStatus';

it('должен вернуть "Действует"', () => {
    const result = getInsuranceStatus(InsuranceStatus.ACTIVE);
    expect(result).toEqual('Действует');
});

it('должен вернуть "Прекратил действие"', () => {
    const result = getInsuranceStatus(InsuranceStatus.EXPIRED);
    expect(result).toEqual('Прекратил действие');
});

it('должен вернуть undefined', () => {
    const result = getInsuranceStatus(InsuranceStatus.UNKNOWN_STATUS);
    expect(result).toBeUndefined();
});
