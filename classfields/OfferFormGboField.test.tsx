import React from 'react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';
import { Car_EngineType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormGboField from './OfferFormGboField';

it('в дефолтном состоянии чекбокс будет отжат', async() => {
    const { findByLabelText } = await renderComponent(<OfferFormGboField/>);
    const checkbox = await findByLabelText('Газобаллонное оборудование') as HTMLInputElement;

    expect(checkbox.checked).toBe(false);
});

it('нажмет чекбокс, если выбран двигатель LPG', async() => {
    const initialValues = {
        [FieldNames.ENGINE_TYPE]: Car_EngineType.LPG,
        [FieldNames.EQUIPMENT]: { gbo: false },
    };
    const { findByLabelText } = await renderComponent(<OfferFormGboField/>, { initialValues });
    const checkbox = await findByLabelText('Газобаллонное оборудование') as HTMLInputElement;

    expect(checkbox.checked).toBe(true);
    expect(checkbox.disabled).toBe(true);
});

it('отправит метрику при нажатии на чекбокс', async() => {
    const { findByLabelText } = await renderComponent(<OfferFormGboField/>);
    const checkbox = await findByLabelText('Газобаллонное оборудование') as HTMLInputElement;

    userEvent.click(checkbox);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledWith({ event: 'click', field: FieldNames.GBO });
});
