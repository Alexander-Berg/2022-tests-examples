jest.mock('auto-core/react/actions/scroll');

import React from 'react';
import _ from 'lodash';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import scrollTo from 'auto-core/react/actions/scroll';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFieldNamesType, OfferFormFields } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import type { Props } from './YearField';
import YearField from './YearField';

const scrollToMock = scrollTo as jest.MockedFunction<typeof scrollTo>;

let defaultProps: Props;

beforeEach(() => {
    defaultProps = {
        suggest: _.range(1999, 2020),
        onFieldChange: jest.fn(),
    };
});

it('выбирает значение из списка', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { findAllByRole } = await renderComponent(<YearField { ...defaultProps }/>, { formApi });

    const [ firstItem ] = await findAllByRole('button');
    userEvent.click(firstItem);

    const value = formApi.current?.getFieldValue(FieldNames.YEAR);
    expect(value).toBe(2019);

    expect(defaultProps.onFieldChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onFieldChange).toHaveBeenCalledWith(FieldNames.YEAR, 2019);
});

it('сворачивает и разворачивает список', async() => {
    const { findAllByRole, findByText } = await renderComponent(<YearField { ...defaultProps }/>);

    let items = await findAllByRole('button');
    expect(items).toHaveLength(16);

    let cutLink = await findByText('Старше');
    userEvent.click(cutLink);

    items = await findAllByRole('button');
    expect(items).toHaveLength(22);

    cutLink = await findByText('Свернуть');
    userEvent.click(cutLink);

    items = await findAllByRole('button');
    expect(items).toHaveLength(16);
    expect(scrollToMock).toHaveBeenCalledTimes(1);
    expect(scrollToMock).toHaveBeenCalledWith(FieldNames.YEAR, { offset: -150 });
});
