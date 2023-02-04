import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_GearType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import type { Props } from './GearTypeField';
import GearTypeField from './GearTypeField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [ Car_GearType.ALL_WHEEL_DRIVE, Car_GearType.FORWARD_CONTROL, Car_GearType.REAR_DRIVE ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    expect.assertions(3);
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<GearTypeField { ...defaultProps }/>, { formApi });

    const item = (await findAllByRole('button')).find(findByChildrenText('Полный'));
    if (!item) {
        return;
    }
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.GEAR_TYPE);
    expect(value).toBe(Car_GearType.ALL_WHEEL_DRIVE);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.GEAR_TYPE, Car_GearType.ALL_WHEEL_DRIVE);
});
