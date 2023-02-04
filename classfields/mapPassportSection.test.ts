import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import type { PersonProfile } from 'auto-core/types/TPersonProfile';

import mapPassportSection from './mapPassportSection';

it('фамилия не менялась', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = {} as PersonProfile;

    personProfile.passport_rf = personProfileMock.passport_rf;

    mapPassportSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('фамилия менялась', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = {} as PersonProfile;

    personProfile.old_name = {
        name_entity: {
            name: 'Александр',
            surname: 'Авдеев',
            patronymic: 'Авдеевич',
        },
    };

    mapPassportSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});
