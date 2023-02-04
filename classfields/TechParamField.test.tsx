import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { nbsp } from 'auto-core/react/lib/html-entities';
import type { FieldErrors, Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import { renderComponent } from 'auto-core/react/components/common/Form/utils/testUtils';

import techParamMock from 'auto-core/models/catalogSuggest/mocks/tech_param.mock';

import type { Props } from './TechParamField';
import TechParamField from './TechParamField';

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: [
            techParamMock.withId('1').withPower(100).value(),
            techParamMock.withId('2').withPower(200).value(),
        ],
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    const formApi = React.createRef<FormContext<FieldNames, Fields, FieldErrors>>();
    const { findByLabelText } = await renderComponent(<TechParamField { ...defaultProps }/>, { formApi });

    const item = await findByLabelText('100 л.с. (1.4 MT)');
    userEvent.click(item);

    const value = formApi.current?.getFieldValue(FieldNames.TECH_PARAM);
    expect(value).toEqual({
        data: '1',
        text: `100${ nbsp }л.с. (1.4 MT)`,
    });

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.TECH_PARAM, value);
});
