import type { PersonProfile } from 'auto-core/types/TPersonProfile';
import type { CreditApplication } from 'auto-core/types/TCreditBroker';
import CreditFormFieldType from 'auto-core/types/TCreditFormField';

import mapDriverLicenseSection from './mapDriverLicenseSection';

it('маппер кредитной формы в формат бекенда / водительское удостоверение', () => {
    const personProfile = {} as PersonProfile;
    const creditApplication = {} as CreditApplication;

    const values = {
        [CreditFormFieldType.DRIVER_LICENSE_NUMBER]: '12 AA 567890',
        [CreditFormFieldType.DRIVER_LICENSE_ISSUE_DATE]: '12.11.2018',
    };

    mapDriverLicenseSection({ values, personProfile, errors: {}, creditApplication });

    expect(personProfile).toMatchSnapshot();
});
