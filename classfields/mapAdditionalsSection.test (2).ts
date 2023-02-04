import { EducationState, MaritalStatusState } from 'auto-core/types/TPersonProfile';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';
import type { PersonProfile } from 'auto-core/types/TPersonProfile';

import mapAdditionalsSection from './mapAdditionalsSection';

it('образование + есть жилье + женат', () => {
    const personProfile = {
        expenses: {
            avg_monthly_expenses: '1234',
        },
    } as PersonProfile;

    const values = {
        [CreditFormFieldType.EDUCATION]: EducationState.PRIMARY,
        [CreditFormFieldType.PROPERTY_OWNERSHIP]: true,
        [ CreditFormFieldType.MARITAL_STATUS]: MaritalStatusState.MARRIED,
    };

    mapAdditionalsSection({ values, personProfile, errors: {} });

    expect(personProfile.education?.state).toEqual(EducationState.PRIMARY);
    expect(personProfile.property_ownership?.has_property).toEqual(true);
    expect(personProfile).not.toHaveProperty('expenses');
});

it('с данными супруга / супурги', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [ CreditFormFieldType.EDUCATION ]: EducationState.PRIMARY,
        [ CreditFormFieldType.PROPERTY_OWNERSHIP ]: true,
        [ CreditFormFieldType.MARITAL_STATUS ]: MaritalStatusState.MARRIED,
        [ CreditFormFieldType.SPOUSE_NAME ]: 'Surname Name Pat',
        [ CreditFormFieldType.SPOUSE_PHONE ]: '79180987656',
        [ CreditFormFieldType.SPOUSE_BIRTH_DATE ]: '23.11.1991',

    };

    mapAdditionalsSection({ values, personProfile, errors: {} });

    expect(personProfile.related_persons?.related_persons).toMatchSnapshot();
});

it('нет жилья', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.PROPERTY_OWNERSHIP]: false,
        [CreditFormFieldType.AVG_MONTHLY_EXPENSES]: 1234,
    };

    mapAdditionalsSection({ values, personProfile, errors: {} });

    expect(personProfile.property_ownership?.has_property).toEqual(false);
    expect(personProfile.expenses?.avg_monthly_expenses).toEqual('1234');
});
