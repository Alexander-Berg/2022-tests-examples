jest.mock('auto-core/react/dataDomain/credit/mappers/addressEntityFromSuggest', () => {
    return () => 'address_entity_mock';
});

import dadataAddressMock from 'auto-core/react/dataDomain/credit/mocks/dadataAddress.mock';
import dadataCompanyMock from 'auto-core/react/dataDomain/credit/mocks/dadataCompany.mock';

import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import { EmploymentType, EmployeePositionType, NotEmployedReason } from 'auto-core/types/TPersonProfile';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapEmploymentSection from './mapEmploymentSection';

const flatCompanyMock = {
    ...dadataCompanyMock.data,
    value: dadataCompanyMock.value,
};

it('тип занятости - в организации', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.EMPLOYMENT_TYPE]: EmploymentType.EMPLOYED,
        [CreditFormFieldType.EMPLOYMENT_COMPANY_NAME]: flatCompanyMock,
        [CreditFormFieldType.EMPLOYMENT_PHONE]: '79168570233',
        [CreditFormFieldType.EMPLOYMENT_POSITION_TYPE]: EmployeePositionType.CHIEF_ACCOUNTANT,
        [CreditFormFieldType.EMPLOYMENT_LAST_EXPERIENCE]: 36,
        [CreditFormFieldType.EMPLOYMENT_ADDRESS]: dadataAddressMock,
    };

    mapEmploymentSection({ values, personProfile, errors: {} });

    expect(personProfile).toMatchSnapshot();
});

it('тип занятости - на себя', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.EMPLOYMENT_TYPE]: EmploymentType.SELF_EMPLOYED,
        [CreditFormFieldType.EMPLOYMENT_SELF_COMPANY_NAME]: flatCompanyMock,
        [CreditFormFieldType.EMPLOYMENT_SELF_PHONE]: '79168570233',
        [CreditFormFieldType.EMPLOYMENT_ADDRESS]: dadataAddressMock,
    };

    mapEmploymentSection({ values, personProfile, errors: {} });

    expect(personProfile).toMatchSnapshot();
});

it('тип занятости - не работаю', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.EMPLOYMENT_TYPE]: EmploymentType.NOT_EMPLOYED,
        [CreditFormFieldType.EMPLOYMENT_UNEMPLOYMENT_REASON]: NotEmployedReason.UNKNOWN_REASON,
        [CreditFormFieldType.EMPLOYMENT_UNEMPLOYMENT_OTHER_REASON]: 'reason_mock',
    };

    mapEmploymentSection({ values, personProfile, errors: {} });

    expect(personProfile).toMatchSnapshot();
});
