import * as assert from 'assert';
import {normalizeVehiclePlate} from '../../app/helpers/normalize-vehicle-plate';

describe('normalizeVehiclePlate', () => {
    it('should convert latin script letters to uppercase', () => {
        assert.strictEqual(normalizeVehiclePlate('asd'), 'ASD');
    });

    it('should convert cyrillic script letters to uppercase latin script', () => {
        assert.strictEqual(normalizeVehiclePlate('123асв'), '123ACB');
    });

    it('should not allow cyrillic letters that have no visual counterpart in latin script', () => {
        assert.strictEqual(normalizeVehiclePlate('123Ф'), null);
    });

    it('should not convert or move digits', () => {
        assert.strictEqual(normalizeVehiclePlate('м049уу77'), 'M049YY77');
    });
});
