import type { InsuranceItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import { INSURANCES } from 'auto-core/react/dataDomain/vinReport/mocks/insurances';

import getInsuranceData from './getInsuranceData';

it('getInsuranceData должен правильно обработать все поля', () => {
    const result = getInsuranceData(INSURANCES[0]);
    expect(result).toMatchSnapshot();
});

it('getInsuranceData должен отфильтровать то, что не удалось достать', () => {
    const result = getInsuranceData({
        from: '1512766800000',
        region_name: 'Урал',
        insurer_name: 'Компания',
    } as InsuranceItem);

    expect(result).toMatchSnapshot();
});
