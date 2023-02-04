import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import getMileage from './getMileage';

it('обычный пробег', () => {
    const result = getMileage({ mileage: 100 });
    expect(result).toEqual('100 км');
});

it('скрученный пробег', () => {
    const result = getMileage({ mileage: 1324243, mileage_status: Status.ERROR });
    expect(result).toEqual('1 324 243 км (мог быть скручен)');
});

it('кривые данные 1, вернет undefined', () => {
    const result = getMileage({ mileage_status: Status.ERROR });
    expect(result).toBeUndefined();
});

it('кривые данные 2, вернет undefined', () => {
    const result = getMileage({ mileage_status: Status.OK });
    expect(result).toBeUndefined();
});

it('странные данные, вернет 0 км', () => {
    const result = getMileage({ mileage: 0, mileage_status: Status.OK });
    expect(result).toEqual('0 км');
});
