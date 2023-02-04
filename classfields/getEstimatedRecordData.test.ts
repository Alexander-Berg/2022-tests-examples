import _ from 'lodash';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import { ESTIMATE_RECORD } from 'auto-core/react/dataDomain/vinReport/mocks/estimates';

import getEstimatedRecordData from './getEstimatedRecordData';

it('getEstimatedRecordData должен вернуть плохой mileage', () => {
    const DATA = _.cloneDeep(ESTIMATE_RECORD);
    DATA.mileage_status = Status.ERROR;
    const result = getEstimatedRecordData(DATA);
    expect(result.find(row => row.key === 'mileage')).toHaveProperty('status', 'ERROR');
});
