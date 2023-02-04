import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import type { Props } from './ColorField';
import ColorField from './ColorField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    expect.assertions(3);
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    await renderComponent(<ColorField { ...defaultProps }/>, { formApi });
    const targetColor = 'FFD600';

    const item = document.querySelector(`[data-id=${ targetColor }]`);
    if (!item) {
        return;
    }
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.COLOR);
    expect(value).toBe(targetColor);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.COLOR, targetColor);
});
