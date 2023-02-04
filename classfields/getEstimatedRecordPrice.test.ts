import _ from 'lodash';

import type { EstimateItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import { ESTIMATE_RECORD } from 'auto-core/react/dataDomain/vinReport/mocks/estimates';

import getEstimatedRecordPrice from './getEstimatedRecordPrice';

it('getEstimatedRecordPrice должен вернуть рейндж', () => {
    const result = getEstimatedRecordPrice(ESTIMATE_RECORD);
    expect(result).toEqual('800 000 ₽ — 1 000 000 ₽');
});

it('getEstimatedRecordPrice должен вернуть число', () => {
    const DATA = _.cloneDeep(ESTIMATE_RECORD);
    if (DATA.results) {
        DATA.results.price_to = 0;
    }
    const result = getEstimatedRecordPrice(DATA);
    expect(result).toEqual('800 000 ₽');
});

it('getEstimatedRecordPrice не сломается, если с данными проблема', () => {
    const result = getEstimatedRecordPrice({ results: {} } as EstimateItem);
    expect(result).toEqual('');
});
