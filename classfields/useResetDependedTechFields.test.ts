jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});

import { renderHook } from '@testing-library/react-hooks';

import { Car_BodyType } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_model';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import catalogSuggestStateMock from 'auto-core/react/dataDomain/catalogSuggest/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import type { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames } from 'auto-core/react/components/common/Form/fields/types';

import type { OfferFormFields, OfferFormFieldNamesType } from 'www-poffer/react/types/offerForm';
import { coreFormContextMock } from 'www-poffer/react/components/common/OfferForm/contexts/CoreFormContext.mock';
import type { AppState } from 'www-poffer/react/store/AppState';

import useResetDependedTechFields from './useResetDependedTechFields';

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<OfferFormFieldNamesType, OfferFormFields, FieldErrors>>;
const { mockUseSelector } = applyUseSelectorMock();

let defaultState: Partial<AppState>;

beforeEach(() => {
    defaultState = {
        catalogSuggest: catalogSuggestStateMock.value(),
        parsedOptions: parsedOptionsMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
        catalogOptions: catalogOptionsMock.value(),
    };
});

it('сбросит все поля, идущие после переданного', () => {
    const initialFields = {
        [FieldNames.MARK]: {
            data: 'KIA',
            text: 'Kia',
        },
        [FieldNames.MODEL]: {
            data: 'RIO',
            text: 'Rio',
        },
        [FieldNames.YEAR]: 2019,
        [FieldNames.SUPER_GEN]: {
            data: '21028015',
            text: 'IV',
        },
        [FieldNames.BODY_TYPE]: Car_BodyType.SEDAN,
    };
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValues: jest.fn(() => initialFields),
    });

    mockUseSelector(defaultState);

    const { result: resetDependedTechFields } = renderHook(() => useResetDependedTechFields());

    resetDependedTechFields?.current(FieldNames.YEAR);
    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledTimes(2);
    expect(coreFormContextMock.setFieldValue).toHaveBeenNthCalledWith(1, FieldNames.BODY_TYPE, null);
    expect(coreFormContextMock.setFieldValue).toHaveBeenNthCalledWith(2, FieldNames.SUPER_GEN, null);
});
