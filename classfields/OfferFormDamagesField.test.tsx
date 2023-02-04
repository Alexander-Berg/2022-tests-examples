import React from 'react';
import userEvent from '@testing-library/user-event';
import { act } from '@testing-library/react';

import '@testing-library/jest-dom';

import { Damage_CarPart, Damage_DamageType } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { Damage } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { Car_BodyType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';
import type { FormContext } from 'auto-core/react/components/common/Form/types';

import DAMAGE_TYPES from 'auto-core/data/damage/Damage.DamageType';

import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { renderComponent } from 'www-poffer/react/utils/testUtils';

import OfferFormDamagesField from './OfferFormDamagesField';

const defaultDamages = {
    car_part: Damage_CarPart.FRONT_BUMPER,
    type: [ Damage_DamageType.DYED ],
    description: 'Все сломано',
} as Damage;

const initialValues = {
    [OfferFormFieldNames.DAMAGES]: [
        defaultDamages,
    ],
};

it('при клике на точку с повреждением открывает попап для добавления повреждения, выбираем чекбокс с повреждением и сохраняем', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();
    const { findByRole, getAllByRole } = await renderComponent(<OfferFormDamagesField/>, { formApi });

    const damageItems = await getAllByRole('button', { name: '+' });

    userEvent.click(damageItems[0]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalled();

    const damageCheckbox = await findByRole('checkbox', { name: DAMAGE_TYPES.DYED });
    const damageTextArea = await findByRole('textbox');

    userEvent.click(damageCheckbox);
    userEvent.type(damageTextArea, 'Все сломано');

    const saveButton = await findByRole('button', { name: 'Сохранить' });

    userEvent.click(saveButton);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.DAMAGES);
    expect(value).toHaveLength(1);
    expect(value).toEqual([ defaultDamages ]);
});

it('при клике на ссылку Удалить удаляем повреждение', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { findByRole, getAllByRole } = await renderComponent(<OfferFormDamagesField/>, { formApi, initialValues });

    const damageItems = await getAllByRole('button', { name: '' });

    expect(damageItems[0].className).toContain('DamageDot_mode_edit');

    userEvent.click(damageItems[0]);

    const removeLink = await findByRole('button', { name: 'Удалить' });

    userEvent.click(removeLink);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.DAMAGES);
    expect(value).toHaveLength(0);

    expect(damageItems[0].className).toContain('DamageDot_mode_add');

});

it('при изменении значений в текущем повреждении обновляет старые значения на новые', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { findByRole, getAllByRole } = await renderComponent(<OfferFormDamagesField/>, { formApi, initialValues });

    const damageItems = await getAllByRole('button', { name: '' });

    userEvent.click(damageItems[0]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalled();

    const damageCheckbox = await findByRole('checkbox', { name: DAMAGE_TYPES.DENT });

    userEvent.click(damageCheckbox);

    const saveButton = await findByRole('button', { name: 'Сохранить' });

    userEvent.click(saveButton);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.DAMAGES);
    const valueDamageType = value && value[0].type;

    expect(value).toHaveLength(1);

    expect(valueDamageType).toContain(Damage_DamageType.DENT);
});

it('если редактируем повреждение, очищая все поля и нажимаем сохранить, то удаляем такое повреждение из списка повреждений', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { findByRole, getAllByRole } = await renderComponent(<OfferFormDamagesField/>, { formApi, initialValues });

    const damageItems = await getAllByRole('button', { name: '' });

    userEvent.click(damageItems[0]);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalled();

    const damageCheckbox = await findByRole('checkbox', { name: DAMAGE_TYPES.DYED });
    const damageTextArea = await findByRole('textbox');

    userEvent.clear(damageTextArea);

    userEvent.click(damageCheckbox);

    const saveButton = await findByRole('button', { name: 'Сохранить' });

    userEvent.click(saveButton);

    const value = formApi.current?.getFieldValue(OfferFormFieldNames.DAMAGES);

    expect(value).toHaveLength(0);

});

it('при изменении кузова в компонент с повреждениями попадает новое значение кузова', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { getByRole } = await renderComponent(<OfferFormDamagesField/>, { formApi, initialValues });

    const scheme = getByRole('figure');

    expect(scheme.className).not.toContain('VehicleBodyDamagesSchemeFrame_body_coupe');

    await act(async() => {
        formApi.current?.setFieldValue(FieldNames.BODY_TYPE, Car_BodyType.COUPE);
    });

    expect(scheme.className).toContain('VehicleBodyDamagesSchemeFrame_body_coupe');

});
