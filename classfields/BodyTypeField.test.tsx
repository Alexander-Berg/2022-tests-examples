import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_BodyType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import type { Props } from './BodyTypeField';
import BodyTypeField from './BodyTypeField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [ Car_BodyType.ALLROAD_5_DOORS, Car_BodyType.CABRIO, Car_BodyType.COUPE ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    expect.assertions(3);
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<BodyTypeField { ...defaultProps }/>, { formApi });

    const item = (await findAllByRole('button')).find(findByChildrenText('Купе'));
    if (!item) {
        return;
    }
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.BODY_TYPE);
    expect(value).toBe(Car_BodyType.COUPE);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.BODY_TYPE, Car_BodyType.COUPE);
});
