import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { findByChildrenText } from 'autoru-frontend/jest/unit/queryHelpers';

import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
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
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<ColorField { ...defaultProps }/>, { formApi });
    const targetColor = 'FFD600';

    const item = (await findAllByRole('button')).find(findByChildrenText('жёлтый'));

    if (!item) {
        return;
    }

    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.COLOR);
    expect(value).toBe(targetColor);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.COLOR, targetColor);
});
