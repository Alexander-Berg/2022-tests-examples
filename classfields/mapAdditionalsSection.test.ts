import type { CreditFormFields } from 'auto-core/react/components/common/CreditForm/types';
import personProfileMock from 'auto-core/react/dataDomain/credit/mocks/personProfile.mock';

import mapAdditionalsSection from './mapAdditionalsSection';

it('маппер секции дополнительно', () => {
    const initialValues = {} as CreditFormFields;

    mapAdditionalsSection({ personProfile: personProfileMock, initialValues });

    expect(initialValues).toMatchSnapshot();
});
