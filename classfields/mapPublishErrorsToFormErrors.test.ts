import { FieldErrors } from 'auto-core/react/components/common/Form/fields/types';

import { OfferFormFieldNames } from 'www-poffer/react/types/offerForm';

import mapPublishErrorsToFormErrors from './mapPublishErrorsToFormErrors';

it('правильно формирует список ошибок', () => {
    const serverErrors = [
        {
            description: 'Неверный формат VIN. Должно быть 17 знаков. Букв O, Q и I не бывает.',
            error_code: 'wrong.vin',
            arguments: [],
            field: 'vin',
        },
        {
            description: 'Вин не прошел проверку',
            error_code: 'wrong.vin',
            arguments: [],
            field: 'vin',
        },
        {
            description: 'Слишком высокая цена',
            error_code: 'wrong.price',
            arguments: [],
            field: 'price',
        },
        {
            description: 'Размещение по четвергам запрещено',
            error_code: 'wrong.weekday',
            arguments: [],
            field: 'weekday',
        },
    ];
    const result = mapPublishErrorsToFormErrors(serverErrors);

    expect(result.knownFieldErrors).toEqual([
        {
            error: {
                text: 'Неверный формат VIN. Должно быть 17 знаков. Букв O, Q и I не бывает.',
                type: FieldErrors.INCORRECT_VALUE,
            },
            field: OfferFormFieldNames.VIN,
        },
        {
            error: {
                text: 'Слишком высокая цена',
                type: FieldErrors.INCORRECT_VALUE,
            },
            field: OfferFormFieldNames.PRICE,
        },
    ]);

    expect(result.unknownFieldErrors).toEqual([
        {
            arguments: [],
            description: 'Размещение по четвергам запрещено',
            error_code: 'wrong.weekday',
            field: 'weekday',
        },
    ]);
});
