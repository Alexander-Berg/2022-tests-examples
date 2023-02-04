import _ from 'lodash';

import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import mapDriverLicense from './mapDriverLicenseSection';

it('маппер кредитной анкеты в форму / водительское удостоверение', () => {
    const initialValues = {} as CreditFormFields;
    const personProfile = _.pick(personProfileMock, 'driver_license');

    mapDriverLicense({ personProfile, initialValues });

    expect(initialValues).toMatchSnapshot();
});
