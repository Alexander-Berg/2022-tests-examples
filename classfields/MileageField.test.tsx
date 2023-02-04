import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import MileageField from './MileageField';

it('меняет значение в инпуте', async() => {
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findByLabelText } = await renderComponent(<MileageField/>, { formApi });

    const input = await findByLabelText('км') as HTMLInputElement;

    expect(input?.value).toBe('');

    userEvent.type(input, '100');

    expect(input?.value).toBe('100');

    const value = formApi.current?.getFieldValue(FieldNames.MILEAGE);
    expect(value).toBe(100);
});

it('не дает ввести больше чем можно', async() => {
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findByLabelText } = await renderComponent(<MileageField/>, { formApi });

    const input = await findByLabelText('км') as HTMLInputElement;

    userEvent.type(input, '100000000000');

    expect(input?.value).toBe('100 000');

    const value = formApi.current?.getFieldValue(FieldNames.MILEAGE);
    expect(value).toBe(100000);
});
