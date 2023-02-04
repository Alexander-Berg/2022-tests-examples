jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});
jest.mock('www-poffer/react/contexts/offerFormPage', () => {
    return {
        useOfferFormPageContext: jest.fn(),
    };
});

import { renderHook } from '@testing-library/react-hooks';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import complectationMock from 'auto-core/models/catalogSuggest/mocks/complectation.mock';
import catalogSuggestMock from 'auto-core/models/catalogSuggest/mocks';

import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';
import type { AppState } from 'www-poffer/react/store/AppState';
import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { useOfferFormPageContext } from 'www-poffer/react/contexts/offerFormPage';
import { offerFormPageContextMock } from 'www-poffer/react/contexts/offerFormPage.mock';

import useSelectComplectation from './useSelectComplectation';

const { mockUseSelector } = applyUseSelectorMock();

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;
const useOfferFormContextMock = useOfferFormPageContext as jest.MockedFunction<typeof useOfferFormPageContext>;

let defaultState: Partial<AppState>;

beforeEach(() => {
    const catalogSuggest = catalogSuggestMock.withComplectations([
        complectationMock.withComplectationId('01').withName('Prestige').value(),
        complectationMock.withComplectationId('02').withName('Elite').withEquipment({
            'led-lights': true, 'mirrors-heat': true, 'steel-wheels': false,
        }).value(),
        complectationMock.withComplectationId('03').withName('Exclusive').value(),
    ]).value();

    defaultState = {
        catalogSuggest: catalogSuggestStateMock.withData(catalogSuggest).value(),
        parsedOptions: parsedOptionsMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
        catalogOptions: catalogOptionsMock.value(),
    };
});

it('меняет комплектацию в форме и добавляет опции для неё, убирая ранее проставленные опции и добавляя те что распарсили', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: OfferFormFieldNamesType) => {
            switch (fieldName) {
                case FieldNames.COMPLECTATION:
                    return '01' as any;
                case FieldNames.EQUIPMENT:
                    return {
                        gbo: true,
                        abs: true,
                        'driver-airbag': true,
                        'passenger-airbag': false,
                    } as any;
                default:
                    return;
            }
        }),
    });
    useOfferFormContextMock.mockReturnValue(offerFormPageContextMock);

    mockUseSelector(defaultState);

    const { result } = renderHook(() => useSelectComplectation());
    result.current('02');

    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledTimes(2);
    expect(coreFormContextMock.setFieldValue).toHaveBeenNthCalledWith(1, FieldNames.COMPLECTATION, '02');
    expect(coreFormContextMock.setFieldValue).toHaveBeenNthCalledWith(2, FieldNames.EQUIPMENT, {
        abs: false,
        aux: true,
        'driver-airbag': false,
        esp: true,
        gbo: true,
        'led-lights': true,
        'mirrors-heat': true,
        'passenger-airbag': false,
        'steel-wheels': false,
    });
    expect(offerFormPageContextMock.sendFormLog).toHaveBeenCalledTimes(1);
});
