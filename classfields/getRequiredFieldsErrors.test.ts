import { REQUIRED_FIELDS } from '../consts/consts';

import getRequiredFieldsErrors from './getRequiredFieldsErrors';

describe('getRequiredFieldsErrors получает человеко-читаемые ошибки из полей формы', () => {
    it('возвращает массив с ошибками, если нет всех обязательных полей', () => {
        const mockFields = {};

        expect(getRequiredFieldsErrors(mockFields, REQUIRED_FIELDS)).toEqual([
            'ВИН',
            'Марка',
            'Модель',
            'Год выпуска',
            'Пробег',
            'Телефон продавца',
            'ID запроса в AMOCRM',
        ]);
    });

    it('возвращает массив с ошибками для пустых обязательных полей', () => {
        const mockFields = {
            vin: { value: '' },
            mark: { value: 'Volga' },
            model: { value: '' },
            year: { value: 1645 },
            run: { value: 20000000 },
            phone: { value: '' },
            amocrm_req_id: { value: '' },
        };

        expect(getRequiredFieldsErrors(mockFields, REQUIRED_FIELDS)).toEqual([
            'ВИН',
            'Модель',
            'Телефон продавца',
            'ID запроса в AMOCRM',
        ]);
    });

    it('возвращает пустой массив, если ошибок нет', () => {
        const mockFields = {
            vin: { value: 'FAKEVIN' },
            mark: { value: 'Volga' },
            model: { value: 'Novaya' },
            year: { value: 1645 },
            run: { value: 20000000 },
            phone: { value: '911' },
            amocrm_req_id: { value: '1' },
        };

        expect(getRequiredFieldsErrors(mockFields, REQUIRED_FIELDS)).toEqual([]);
    });
});
