import React from 'react';
import userEvent from '@testing-library/user-event';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import offerDraftMock from 'www-poffer/react/dataDomain/offerDraft/mock';
import { renderComponent } from 'www-poffer/react/utils/testUtils';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';
import type { AppState } from 'www-poffer/react/store/AppState';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';
import { FormFeatures } from 'www-poffer/react/contexts/offerFormPage';

import EquipmentByVin from './EquipmentByVin';

const VIN = 'WBAJC51090B319650';

let defaultState: Partial<AppState>;
let initialValues: OfferFormFields;

beforeEach(() => {
    initialValues = {
        [OfferFormFieldNames.VIN]: VIN,
        [OfferFormFieldNames.OPTIONS_UPDATED]: false,
    };

    defaultState = {
        equipmentDictionary: equipmentDictionaryMock,
        catalogOptions: catalogOptionsMock.value(),
        parsedOptions: parsedOptionsMock.value(),
        offerDraft: offerDraftMock
            .withOfferMock(cloneOfferWithHelpers(offerMock).withVin(VIN))
            .withPartnerOptions({
                esp: true,
                'airbag-driver': false,
                aux: true,
                abs: true,
            })
            .value(),
    };
});

describe('показ', () => {
    it('покажет блок если в сторе есть опции для выбора', async() => {
        const { getByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues });
        const title = getByText(/мы нашли/i);

        expect(title).not.toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(1, { feature: FormFeatures.VIN_OPTIONS, level_5: 'show' });
    });

    it('не покажет блок если в сторе нет опций для выбора', async() => {
        const state = {
            ...defaultState,
            offerDraft: offerDraftMock
                .withOfferMock(cloneOfferWithHelpers(offerMock).withVin(VIN))
                .withPartnerOptions({}).value(),
        };
        const { queryByText } = await renderComponent(<EquipmentByVin/>, { state, initialValues });
        const title = queryByText(/мы нашли/i);

        expect(title).toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });

    it('не покажет блок если опции уже были проапдейтчены на бэке', async() => {
        const customInitialValues = {
            ...initialValues,
            [OfferFormFieldNames.OPTIONS_UPDATED]: true,
        };
        const { queryByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues: customInitialValues });
        const title = queryByText(/мы нашли/i);

        expect(title).toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });

    it('не покажет блок если вин в драфте не совпадает с вином в форме', async() => {
        const customInitialValues = {
            ...initialValues,
            [OfferFormFieldNames.VIN]: 'Z8TND5FS9DM047548',
        };
        const { queryByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues: customInitialValues });
        const title = queryByText(/мы нашли/i);

        expect(title).toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });

    it('не покажет блок если все опции были выбраны в ручную пользователем', async() => {
        const customInitialValues = {
            ...initialValues,
            [FieldNames.EQUIPMENT]: {
                abs: true,
                'airbag-driver': false,
                aux: true,
                esp: true,
                gbo: true,
            },
        };
        const { queryByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues: customInitialValues });
        const title = queryByText(/мы нашли/i);

        expect(title).toBeNull();
        expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(0);
    });
});

describe('правильно формирует заголовок', () => {
    it('если пользователь не выбирал другие опции', async() => {
        const { getByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues });
        const title = getByText(/мы нашли 3 опции по VIN/i);

        expect(title).not.toBeNull();
    });

    it('если пользователь выбирал другие опции', async() => {
        const customInitialValues = {
            ...initialValues,
            [FieldNames.EQUIPMENT]: {
                esp: true,
                gbo: true,
            },
        };
        const { getByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues: customInitialValues });
        const title = getByText(/мы нашли ещё 2 опции по VIN/i);

        expect(title).not.toBeNull();
    });
});

it('открывает шторку', async() => {
    const { getAllByRole, getByRole, queryByRole } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues });

    let title = queryByRole('heading', { name: /опции/i });
    expect(title).toBeNull();

    const buttonShow = getByRole('button', { name: /посмотреть/i });
    userEvent.click(buttonShow);

    title = getByRole('heading', { name: /опции/i });

    expect(title).not.toBeNull();
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(2);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(2, { feature: FormFeatures.VIN_OPTIONS, level_5: 'click' });

    const checkboxes = (getAllByRole('checkbox') as Array<HTMLInputElement>).map((checkbox) => checkbox.value);

    expect(checkboxes).toEqual([ 'abs', 'esp', 'aux' ]);
});

it('при выборе опций передает их в форму, ставит флаг про адейт и скрывает блок', async() => {
    const customInitialValues = {
        ...initialValues,
        [FieldNames.EQUIPMENT]: {
            gbo: true,
        },
    };
    const formApi = React.createRef<FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>();

    const { getByRole, queryByText } = await renderComponent(<EquipmentByVin/>, { state: defaultState, initialValues: customInitialValues, formApi });

    const buttonShow = getByRole('button', { name: /посмотреть/i });
    userEvent.click(buttonShow);

    const espCheckbox = getByRole('checkbox', { name: /система стабилизации/i }) as HTMLInputElement;
    userEvent.click(espCheckbox);

    const buttonSave = getByRole('button', { name: /добавить/i });
    userEvent.click(buttonSave);

    const title = queryByText(/мы нашли/i);

    expect(title).toBeNull();
    expect(formApi.current?.getFieldValue(OfferFormFieldNames.OPTIONS_UPDATED)).toBe(true);
    expect(formApi.current?.getFieldValue(FieldNames.EQUIPMENT)).toEqual({
        abs: true,
        aux: true,
        gbo: true,
    });
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(3);
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenNthCalledWith(3, { feature: FormFeatures.VIN_OPTIONS, level_5: 'save' });
});
