import _ from 'lodash';

import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import type { PersonProfile } from 'auto-core/types/TPersonProfile';

import mapAddressSection from './mapAddressSection';

it('с местом проживания по месту регистрации', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [ 'residence_address', 'registration_address' ]);

    personProfile.residence_address = personProfile.registration_address;

    mapAddressSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('с разными местом проживания и местом регистрации', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [ 'residence_address', 'registration_address' ]) as PersonProfile;

    personProfile.residence_address!.address_entity.street = 'Тестовая';

    mapAddressSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});
