import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_Transmission } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import type { Props } from './TransmissionField';
import TransmissionField from './TransmissionField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [ Car_Transmission.AUTOMATIC, Car_Transmission.MECHANICAL, Car_Transmission.ROBOT ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    expect.assertions(3);
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<TransmissionField { ...defaultProps }/>, { formApi });

    const item = (await findAllByRole('button')).find(findByChildrenText('Механика'));
    if (!item) {
        return;
    }
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.TRANSMISSION);
    expect(value).toBe(Car_Transmission.MECHANICAL);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.TRANSMISSION, Car_Transmission.MECHANICAL);
});
