import type { CarSuggest } from '@vertis/schema-registry/ts-types-snake/auto/api/cars_helper_model';

import getTechParamNameParts from 'auto-core/react/lib/catalogSuggest/getTechParamNameParts';

import type { ArrayElement } from 'auto-core/types/ArrayElement';

import techParams from './mocks/techParams.mock';

it('Должен вернуть getEngineNameplate для Электро, если нет nameplate_engine', () => {
    expect(getTechParamNameParts(techParams.withoutNameplateEngine as ArrayElement<CarSuggest['tech_params']>).engineNameplate).toEqual('Electro');
});

it('Должен вернуть getEngineNameplate для Электро, если есть nameplate_engine', () => {
    expect(getTechParamNameParts(techParams.withNameplateEngine as ArrayElement<CarSuggest['tech_params']>).engineNameplate).toEqual('Long Range Electro');
});
