import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import superGenMock from 'auto-core/models/catalogSuggest/mocks/super_gen.mock';

import type { Props } from './SuperGenField';
import SuperGenField from './SuperGenField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [
            superGenMock.withId('1').withName('IX (X110)').value(),
            superGenMock.withId('2').withName('X (X200)').value(),
        ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findByLabelText } = await renderComponent(<SuperGenField { ...defaultProps }/>, { formApi });

    const item = await findByLabelText('IX (X110)');
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.SUPER_GEN);
    expect(value).toEqual({
        data: '1',
        text: 'IX (X110)',
    });

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.SUPER_GEN, value);
});
