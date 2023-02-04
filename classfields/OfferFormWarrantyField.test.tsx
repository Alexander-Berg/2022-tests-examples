import React from 'react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { waitFor } from '@testing-library/react';
import MockDate from 'mockdate';

import selectItemInSelect from 'autoru-frontend/jest/unit/selectItemInSelect';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';

import OfferFormWarrantyField from './OfferFormWarrantyField';

beforeEach(() => {
    MockDate.set('2020-05-20');
});

afterEach(() => {
    MockDate.reset();
});

it('при выборе На гарантии отсылается метрика', async() => {

    const { getByRole } = await renderComponent(<OfferFormWarrantyField/>);

    const warrantyCheckbox = getByRole('checkbox');

    userEvent.click(warrantyCheckbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});

it('если выставили чекбокс на гарантии, то показываем селекты месяцев и лет', async() => {
    const { getAllByRole, getByRole } = await renderComponent(<OfferFormWarrantyField/>);

    const warrantyCheckbox = getByRole('checkbox');

    userEvent.click(warrantyCheckbox);

    await waitFor(() => {
        const selects = getAllByRole('listbox');
        expect(selects).toHaveLength(2);
    });
});

it('если выбрали текущий год, то сбрасываем выбранный месяц', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const initialValues = {
        [OfferFormFieldNames.WARRANTY]: {
            month: 6,
            warranty: true,
            year: 2024,
        },
    };

    const { getByRole } = await renderComponent(
        <OfferFormWarrantyField/>, { initialValues, formApi });

    const yearSelect = getByRole('button', { name: '2024' });
    const monthSelect = getByRole('button', { name: 'Июнь' });

    await selectItemInSelect(yearSelect, '2020');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.WARRANTY)).toEqual({
        month: null,
        warranty: true,
        year: 2020,
    });

    await selectItemInSelect(monthSelect, 'Июль');

    await selectItemInSelect(yearSelect, '2025');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.WARRANTY)).toEqual({
        month: 7,
        warranty: true,
        year: 2025,
    });

});

it('если выбрали год, то отсылаем метрики', async() => {
    const initialValues = {
        [OfferFormFieldNames.WARRANTY]: {
            warranty: true,
        },
    };

    const { getByRole } = await renderComponent(
        <OfferFormWarrantyField/>, { initialValues },
    );

    const yearSelect = getByRole('button', { name: 'Год окончания' });

    await selectItemInSelect(yearSelect, '2024');

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);

});

it('если выбрали месяц, то отсылаем метрики', async() => {
    const initialValues = {
        [OfferFormFieldNames.WARRANTY]: {
            warranty: true,
        },
    };

    const { getByRole } = await renderComponent(
        <OfferFormWarrantyField/>, { initialValues },
    );

    const monthSelect = getByRole('button', { name: 'Месяц' });

    await selectItemInSelect(monthSelect, 'Апрель');

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);

});
