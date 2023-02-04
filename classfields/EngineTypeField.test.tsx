import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_EngineType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';

import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import type { Props } from './EngineTypeField';
import EngineTypeField from './EngineTypeField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [ Car_EngineType.ELECTRO, Car_EngineType.GASOLINE, Car_EngineType.DIESEL ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    expect.assertions(3);
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<EngineTypeField { ...defaultProps }/>, { formApi });

    const item = (await findAllByRole('button')).find(findByChildrenText('Бензин'));
    if (!item) {
        return;
    }
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.ENGINE_TYPE);
    expect(value).toBe(Car_EngineType.GASOLINE);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.ENGINE_TYPE, Car_EngineType.GASOLINE);
});
