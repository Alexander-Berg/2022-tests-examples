import {
    AddressValidationWithNotRequiredDadata,
    AddressEntityValidationWithNotRequiredDadata,
    AddressValidation,
    AddressEntityValidation,
} from 'auto-core/react/dataDomain/credit/validators/addressValidator';

const dadataFields = [
    'region_with_type',
    'area_with_type',
    'city_with_type',
    'city_district_with_type',
    'street_with_type',
    'settlement_with_type',
    'house',
    'block_type_full',
    'block_type',
    'block',
    'flat',
];

const quotasCaseAddress = {
    data: {
        region_with_type: 'Красноярский край',
        area_with_type: null,
        city_with_type: 'г Красноярск',
        city_district_with_type: null,
        street_with_type: 'Ленинский р-н',
        settlement_with_type: 'ж/д_ст "Кривозеровка"',
        house: '27',
        block_type_full: 'строение',
        block_type: 'стр',
        block: '74',
        flat: 'VII комн 33 оф 536',
    },
    value: 'г Красноярск, пр-кт им.газеты "Красноярский рабочий", д 27 стр 74, VII комн 33 оф 536',
};

describe('AddressValidationWithNotRequiredDadata - адрес с возможностью ввода руками без саджеста', () => {
    it('все поля могут быть null', () => {
        const data: { [key: string]: string | null } = {};

        dadataFields.reduce((ret, fieldName) => {
            ret[fieldName] = null;

            return ret;
        }, data);

        expect(() => {
            AddressValidationWithNotRequiredDadata.validateSync(data);
        }).not.toThrow();
    });

    it('пропускает отсутствие data (саджеста)', () => {
        expect(() => {
            AddressEntityValidationWithNotRequiredDadata.validateSync({
                value: 'адрес',
            });
        }).not.toThrow();
    });

    it('кавычки в адресе (улице) валидны', () => {
        expect(() => {
            AddressEntityValidationWithNotRequiredDadata.validateSync(quotasCaseAddress);
        }).not.toThrow();
    });
});

describe('AddressValidation - адрес без возможности ручного ввода (только саджест)', () => {
    it('все поля могут быть null', () => {
        const data: { [key: string]: string | null } = {};

        dadataFields.reduce((ret, fieldName) => {
            ret[fieldName] = null;

            return ret;
        }, data);

        expect(() => {
            AddressValidation.validateSync(data);
        }).toThrow();
    });

    it('НЕ пропускает отсутствие data (саджеста)', () => {
        expect(() => {
            AddressEntityValidation.validateSync({
                value: 'адрес',
            });
        }).toThrow();
    });

    it('кавычки в адресе (улице) и поселении валидны, _ в поселении тоже', () => {
        expect(() => {
            AddressEntityValidation.validateSync(quotasCaseAddress);
        }).not.toThrow();
    });
});
