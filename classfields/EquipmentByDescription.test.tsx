jest.mock('auto-core/react/lib/localstorage', () => ({
    getItem: jest.fn(),
}));
jest.mock('./utils/useParseOptions');
jest.mock('auto-core/react/dataDomain/parsedOptions/actions/updatedDeselectedOptions');
jest.mock('www-poffer/react/utils/storeDeselectedOptions');

import React from 'react';
import userEvent from '@testing-library/user-event';
import flushPromises from 'jest/unit/flushPromises';

import updatedDeselectedOptions from 'auto-core/react/dataDomain/parsedOptions/actions/updatedDeselectedOptions';
import { getItem } from 'auto-core/react/lib/localstorage';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import storeDeselectedOptions from 'www-poffer/react/utils/storeDeselectedOptions';
import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { AppState } from 'www-poffer/react/store/AppState';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { FormFeatures } from 'www-poffer/react/contexts/offerFormPage';

import EquipmentByDescription from './EquipmentByDescription';
import useParseOptions from './utils/useParseOptions';

const getItemMock = getItem as jest.MockedFunction<typeof getItem>;
const useParseOptionsMock = useParseOptions as jest.MockedFunction<typeof useParseOptions>;

const successfulParseOptionMock = () => Promise.resolve([ 'usb', 'computer' ]);
const unsuccessfulParseOptionMock = () => Promise.resolve([]);

let defaultState: Partial<AppState>;
let initialValues: OfferFormFields;

beforeEach(() => {
    initialValues = {
        [OfferFormFieldNames.DESCRIPTION]: 'имеется usb и бортовой компьютер',
        [FieldNames.EQUIPMENT]: {
            gbo: true,
            aux: false,
        },
    };

    defaultState = {
        equipmentDictionary: equipmentDictionaryMock,
        offerDraft: offerDraftMock.value(),
        parsedOptions: parsedOptionsMock.value(),
        catalogOptions: catalogOptionsMock.value(),
    };

    useParseOptionsMock.mockImplementation(() => successfulParseOptionMock);
    getItemMock.mockReturnValue(null);
});

describe('показ', () => {
    it('покажет блок если ручка прислала данные', async() => {
        const { getByText } = await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues });

        await flushPromises();
        const title = getByText(/мы нашли/i);

        expect(title).not.toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { feature: FormFeatures.DESCRIPTION_OPTIONS, level_5: 'show' });
    });

    it('не покажет блок если ручка не прислала данные', async() => {
        useParseOptionsMock.mockImplementation(() => unsuccessfulParseOptionMock);

        const { queryByText } = await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues });

        await flushPromises();
        const title = queryByText(/мы нашли/i);

        expect(title).toBeNull();
    });
});

it('при маунте сохранить данные из ЛС в стор', async() => {
    getItemMock.mockReturnValue(JSON.stringify({ draft_id: [ 'usb', 'computer' ] }));

    await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues });

    expect(updatedDeselectedOptions).toHaveBeenCalledTimes(1);
    expect(updatedDeselectedOptions).toHaveBeenCalledWith({ usb: false, computer: false });
});

it('открывает шторку', async() => {
    const { getAllByRole, getByRole, queryByRole } = await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues });

    let title = queryByRole('heading', { name: /опции/i });
    expect(title).toBeNull();

    const buttonShow = getByRole('button', { name: /посмотреть/i });
    userEvent.click(buttonShow);

    title = getByRole('heading', { name: /опции/i });

    expect(title).not.toBeNull();
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { feature: FormFeatures.DESCRIPTION_OPTIONS, level_5: 'click' });

    const checkboxes = (getAllByRole('checkbox') as Array<HTMLInputElement>).map((checkbox) => checkbox.value);

    expect(checkboxes).toEqual([ 'usb', 'computer' ]);
});

it('при сбросе опций в шторке отпарвит метрику', async() => {
    const { getByRole } = await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues });

    const buttonShow = getByRole('button', { name: /посмотреть/i });
    userEvent.click(buttonShow);

    const buttonReset = getByRole('button', { name: /сбросить/i });
    userEvent.click(buttonReset);

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(3);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { feature: FormFeatures.DESCRIPTION_OPTIONS, level_5: 'reset' });
});

it('при сохранении в шторке, апдейтит поля в форме и отправляет метрики', async() => {
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { getByRole, queryByText } = await renderComponent(<EquipmentByDescription/>, { state: defaultState, initialValues, formApi });

    const buttonShow = getByRole('button', { name: /посмотреть/i });
    userEvent.click(buttonShow);

    const usbCheckbox = getByRole('checkbox', { name: /usb/i }) as HTMLInputElement;
    userEvent.click(usbCheckbox);

    const buttonSave = getByRole('button', { name: /сохранить/i });
    userEvent.click(buttonSave);

    expect(formApi.current?.getFieldValue(FieldNames.EQUIPMENT)).toEqual({
        gbo: true,
        aux: false,
        usb: false,
    });
    expect(storeDeselectedOptions).toHaveBeenCalledTimes(1);
    expect(storeDeselectedOptions).toHaveBeenCalledWith({ usb: false }, 'draft_id');

    expect(updatedDeselectedOptions).toHaveBeenCalledTimes(2);
    expect(updatedDeselectedOptions).toHaveBeenNthCalledWith(2, { usb: false });

    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(3);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { feature: FormFeatures.DESCRIPTION_OPTIONS, level_5: 'save' });

    const title = queryByText(/мы нашли 1 опцию в описании и указали их в объявлении/i);
    expect(title).not.toBeNull();
});
