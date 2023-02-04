jest.mock('auto-core/react/dataDomain/credit/mappers/addressEntityFromSuggest', () => {
    return () => 'address_entity_mock';
});

import dadataAddressMock from 'auto-core/react/dataDomain/credit/mocks/dadataAddress.mock';
import dadataFmsUnitMock from 'auto-core/react/dataDomain/credit/mocks/dadataFmsUnit.mock';

import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import type { CreditApplication } from 'auto-core/types/TCreditBroker';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapPassportSection from './mapPassportSection';

const flatAddressMock = {
    ...dadataAddressMock.data,
    value: dadataAddressMock.value,
};

it('тип занятости - в организации', () => {
    const personProfile = {} as PersonProfile;
    const creditApplication = {
        borrower_person_profile: { name: { name_entity: { name: 'name', patronymic: 'patronymic' } } },
    } as CreditApplication;

    const values = {
        [CreditFormFieldType.PASSPORT_RF_FULL_NUMBER]: '1234-567890',
        [CreditFormFieldType.PASSPORT_RF_DEPART_CODE]: '111-000',
        [CreditFormFieldType.PASSPORT_RF_DEPART_NAME]: dadataFmsUnitMock,
        [CreditFormFieldType.PASSPORT_RF_ISSUE_DATE]: '10.12.2019',
        [CreditFormFieldType.BIRTH_DATE]: '10.12.2019',
        [CreditFormFieldType.BIRTH_PLACE]: flatAddressMock,
        [CreditFormFieldType.NO_OLD_NAME]: false,
        [CreditFormFieldType.OLD_SURNAME]: 'surname',
    };

    mapPassportSection({ values, personProfile, errors: {}, creditApplication });

    expect(personProfile).toMatchSnapshot();
});
