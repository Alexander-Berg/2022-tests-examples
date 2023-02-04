jest.mock('auto-core/react/components/common/Form/contexts/FormContext', () => {
    return {
        useFormContext: jest.fn(),
    };
});

import { renderHook } from '@testing-library/react-hooks';

import applyUseSelectorMock from 'autoru-frontend/jest/unit/applyUseSelectorMock';

import type { FormContext } from 'auto-core/react/components/common/Form/types';
import { useFormContext } from 'auto-core/react/components/common/Form/contexts/FormContext';
import catalogOptionsMock from 'auto-core/react/dataDomain/catalogOptions/mock';
import parsedOptionsMock from 'auto-core/react/dataDomain/parsedOptions/mock';
import equipmentDictionaryMock from 'auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary';
import type { Fields } from 'auto-core/react/components/common/Form/fields/types';
import { FieldNames, FieldErrors } from 'auto-core/react/components/common/Form/fields/types';
import { formContextMock } from 'auto-core/react/components/common/Form/contexts/FormContext.mock';
import type { AppStateCore } from 'auto-core/react/AppState';

import useSetEquipmentValue from './useSetEquipmentValue';

const { mockUseSelector } = applyUseSelectorMock();

const useFormContextMock = useFormContext as jest.MockedFunction<() => FormContext<FieldNames, Fields, FieldErrors>>;
const coreFormContextMock = {
    ...formContextMock,
    requiredErrorType: FieldErrors.REQUIRED,
};

let defaultState: Partial<AppStateCore>;

beforeEach(() => {
    defaultState = {
        catalogOptions: catalogOptionsMock.value(),
        equipmentDictionary: equipmentDictionaryMock,
        parsedOptions: parsedOptionsMock.value(),
    };
});

it('при стратегии reset сбросит текущие поля кроме ГБО и установит новые', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: FieldNames) => {
            switch (fieldName) {
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

    mockUseSelector(defaultState);

    const { result } = renderHook(() => useSetEquipmentValue());
    result.current({ esp: true, aux: false, 'led-lights': true }, 'reset');

    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledTimes(1);
    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledWith(FieldNames.EQUIPMENT, {
        abs: false,
        'driver-airbag': false,
        gbo: true,
        'led-lights': true,
        'passenger-airbag': false,
        esp: true,
        aux: false,
    });
});

it('при стратегии merge не сбросит поля в группах без мультиселектов и смержит опции в группах с ними', () => {
    useFormContextMock.mockReturnValue({
        ...coreFormContextMock,
        getFieldValue: jest.fn((fieldName: FieldNames) => {
            switch (fieldName) {
                case FieldNames.EQUIPMENT:
                    return {
                        gbo: true,
                        abs: true,
                        'driver-airbag': true, // оно в группе с мультивыбором
                        'laser-lights': true, // оно в группе без мультивыбора
                        'steklo-auto': true, // оно в группе без мультивыбора, проверяем что оно не сброситься
                    } as any;
                default:
                    return;
            }
        }),
    });

    mockUseSelector(defaultState);

    const { result } = renderHook(() => useSetEquipmentValue());
    result.current({ esp: true, aux: false, 'led-lights': true, 'airbag-passenger': true }, 'merge');

    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledTimes(1);
    expect(coreFormContextMock.setFieldValue).toHaveBeenCalledWith(FieldNames.EQUIPMENT, {
        abs: true,
        'driver-airbag': true, // помержилось
        'airbag-passenger': true, // помержилось
        gbo: true,
        'laser-lights': true, // не сбросилось
        // 'led-lights': true, // не должно проставиться
        esp: true,
        aux: false,
        'steklo-auto': true,
    });
});
