import React from 'react';
import '@testing-library/jest-dom';
import MockDate from 'mockdate';
import { act } from '@testing-library/react';

import selectItemInSelect from 'autoru-frontend/jest/unit/selectItemInSelect';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';

beforeEach(() => {
    MockDate.set('2023-05-20');
});

afterEach(() => {
    MockDate.reset();
});

import OfferFormPurchaseDateField from './OfferFormPurchaseDateField';

it('когда меняют год выпуска машины сердито сбрасываем год покупки, если он меньше года выпуска', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const initialValues = {
        [FieldNames.YEAR]: 1980,
        [OfferFormFieldNames.PURCHASE_DATE]: {
            month: null,
            year: 2020,
        },
    };

    const { getByRole } = await renderComponent(
        <OfferFormPurchaseDateField/>, { initialValues, formApi });

    const yearSelect = getByRole('button', { name: '2020' });

    await act(async() => {
        formApi.current?.setFieldValue(FieldNames.YEAR, 2021);
    });

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PURCHASE_DATE)).toEqual({
        year: null,
        month: null,
    });

    await selectItemInSelect(yearSelect, '2023');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PURCHASE_DATE)).toEqual({
        year: 2023,
        month: null,
    });
});

it('если выбрали текущий год, то сбрасываем выбранный месяц', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const initialValues = {
        [OfferFormFieldNames.PURCHASE_DATE]: {
            month: 6,
            year: 2020,
        },
    };

    const { getByRole } = await renderComponent(
        <OfferFormPurchaseDateField/>, { initialValues, formApi });

    const yearSelect = getByRole('button', { name: '2020' });
    const monthSelect = getByRole('button', { name: 'Июнь' });

    await selectItemInSelect(yearSelect, '2023');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PURCHASE_DATE)).toEqual({
        month: null,
        year: 2023,
    });
    await selectItemInSelect(yearSelect, '2022');

    await selectItemInSelect(monthSelect, 'Июль');

    expect(formApi.current?.getFieldValue(OfferFormFieldNames.PURCHASE_DATE)).toEqual({
        month: 7,
        year: 2022,
    });
});

it('если выбрали год, то отсылаем метрики', async() => {
    const { getByRole } = await renderComponent(
        <OfferFormPurchaseDateField/>,
    );

    const yearSelect = getByRole('button', { name: 'Год' });

    await selectItemInSelect(yearSelect, '2021');

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);

});

it('если выбрали месяц, то отсылаем метрики', async() => {
    const { getByRole } = await renderComponent(
        <OfferFormPurchaseDateField/>,
    );

    const monthSelect = getByRole('button', { name: 'Месяц' });

    await selectItemInSelect(monthSelect, 'Апрель');

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);

});
