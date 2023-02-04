import _ from 'lodash';

import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import type { CreditApplication } from 'auto-core/types/TCreditBroker';
import type { PersonProfile } from 'auto-core/types/TPersonProfile';

import mapProfileSection from './mapProfileSection';

it('маппер профиля пользователя', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [
        'name',
        'emails',
        'phones',
        'dependents',
    ]) as PersonProfile;

    mapProfileSection({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});

it('маппер профиля пользователя не падает, когда нет creditApplication.info', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, [
        'name',
        'emails',
        'phones',
        'dependents',
    ]) as PersonProfile;

    expect(() => {
        mapProfileSection({ personProfile, initialValues, creditApplication: {} as CreditApplication });
    }).not.toThrow();
});
