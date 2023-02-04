jest.mock('auto-core/react/dataDomain/credit/mappers/suggestsFromAddressEntity', () => {
    return () => {
        return { value: 'address_entity_mock' };
    };
});

import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileEmployedMock from 'auto-core/react/dataDomain/credit/mocks/personProfileEmployed.mock';
import personProfileSelfEmployedMock from 'auto-core/react/dataDomain/credit/mocks/personProfileSelfEmployed.mock';

import { NotEmployedReason } from 'auto-core/types/TPersonProfile';
import type { PersonProfile } from 'auto-core/types/TPersonProfile';

import mapEmploymentSection from './mapEmploymentSection';

it('место работы - в организации', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = {} as PersonProfile;

    personProfile.employment = personProfileEmployedMock;

    mapEmploymentSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('место работы - на себя', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = {} as PersonProfile;

    personProfile.employment = personProfileSelfEmployedMock;

    mapEmploymentSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('место работы - не работаю', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = {} as PersonProfile;

    personProfile.employment = {
        not_employed: {
            other_reason: 'Сори, не хочуу',
            reason: NotEmployedReason.UNKNOWN_REASON,
        },
    };

    mapEmploymentSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});
