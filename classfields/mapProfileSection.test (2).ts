import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import type { CreditApplication } from 'auto-core/types/TCreditBroker';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapProfileSection from './mapProfileSection';

it('маппер кредитной формы в формат бекенда / личный профиль', () => {
    const personProfile = {} as PersonProfile;
    const creditApplication = {} as CreditApplication;

    const values = {
        [CreditFormFieldType.PROFILE_NAME]: {
            name: 'name',
            surnamr: 'surname',
            patronymic: 'patronymic',
            value: 'surname name patronymic',
        },
        [CreditFormFieldType.PROFILE_EMAIL]: 'test@test.test',
        [CreditFormFieldType.PROFILE_PHONE]: '79168570322',
        [CreditFormFieldType.AMOUNT_OF_CHILDREN]: '2',
    };

    mapProfileSection({ values, personProfile, errors: {}, creditApplication });

    expect(personProfile).toMatchSnapshot();
});
