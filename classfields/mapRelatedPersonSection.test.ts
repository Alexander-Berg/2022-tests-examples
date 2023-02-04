import _ from 'lodash';

import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import type { PersonProfile, PhoneEntity } from 'auto-core/types/TPersonProfile';

import mapRelatedPersonSection from './mapRelatedPersonSection';

it('дополнительный контакт - другого человека', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [
        'related_persons',
        'phones',
    ]) as PersonProfile;

    mapRelatedPersonSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('дополнительный контакт - свой', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [
        'phones',
    ]) as PersonProfile;

    const additionalPhoneEntity = {
        phone: '79168570422',
        phone_type: 'ADDITIONAL',
    } as PhoneEntity;

    personProfile.phones?.phone_entities.push(additionalPhoneEntity);

    mapRelatedPersonSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});
