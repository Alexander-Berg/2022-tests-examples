import { renderHook } from '@testing-library/react-hooks';

import { PtsStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import useValidator from './useValidator';

it('поле не обязательное если нет ПТС', async() => {
    const { result } = renderHook(() => useValidator(PtsStatus.NO_PTS));

    const validationResult = await result.current?.(0, {});
    expect(validationResult).toBe(undefined);
});

it('поле обязательное если есть ПТС', async() => {
    const { result } = renderHook(() => useValidator(PtsStatus.ORIGINAL));

    const validationResult = await result.current?.(0, {});
    expect(validationResult).toEqual({
        text: 'Укажите количество владельцев',
        type: FieldErrors.REQUIRED,
    });
});
