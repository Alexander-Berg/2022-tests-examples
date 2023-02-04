import type { OwnerItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import getOwnerType from './getOwnerType';

it('getOwnerType возвращает корретное название физлица', () => {
    const result = getOwnerType({ owner_type: { type: 'PERSON' } } as OwnerItem);
    expect(result).toEqual('Физическое лицо');
});

it('getOwnerType возвращает корретное название юрлица', () => {
    const result = getOwnerType({ owner_type: { type: 'LEGAL' } } as OwnerItem);
    expect(result).toEqual('Юридическое лицо');
});

it('getOwnerType возвращает корретное название без регистрации', () => {
    const result = getOwnerType({ owner_type: { type: 'UNKNOWN_OWNER_TYPE' } } as OwnerItem);
    expect(result).toEqual('Без регистрации');
});
