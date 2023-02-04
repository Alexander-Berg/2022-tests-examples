import { RelatedPersonType } from 'auto-core/types/TPersonProfile';
import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapRelatedPersonSection from './mapRelatedPersonSection';

it('дополнительный номер - свой', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.RELATED_PERSON_TYPE]: RelatedPersonType.UNKNOWN_TYPE,
        [CreditFormFieldType.RELATED_PERSON_PHONE]: '79168570322',
    };

    mapRelatedPersonSection({ values, personProfile, errors: {} });

    expect(personProfile).toMatchSnapshot();
});

it('дополнительный номер - другого человека', () => {
    const personProfile = {} as PersonProfile;

    const values = {
        [CreditFormFieldType.RELATED_PERSON_TYPE]: RelatedPersonType.FRIEND,
        [CreditFormFieldType.RELATED_PERSON_PHONE]: '79168570322',
        [CreditFormFieldType.RELATED_PERSON_NAME]: {
            name: 'name',
            surnamr: 'surname',
            patronymic: 'patronymic',
            value: 'surname name patronymic',
        },
    };

    mapRelatedPersonSection({ values, personProfile, errors: {} });

    expect(personProfile).toMatchSnapshot();
});
