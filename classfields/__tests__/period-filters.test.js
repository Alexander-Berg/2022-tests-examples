import MockDate from 'mockdate';

import { getPeriodBackendParamsByQuery } from 'app/lib/period-filters';

describe('moveTo', () => {
    it('generates inclusive time period', () => {
        expect(getPeriodBackendParamsByQuery({
            startTime: '20-01-2018',
            endTime: '01-12-2018'
        })).toEqual({
            startTime: '2018-01-19T21:00:00.000Z',
            endTime: '2018-12-01T20:59:59.999Z'
        });
    });

    it('uses 14-day period if query is empty', () => {
        MockDate.set('01-01-2018');
        expect(getPeriodBackendParamsByQuery()).toEqual({
            startTime: '2017-12-18T21:00:00.000Z',
            endTime: '2018-01-01T20:59:59.999Z'
        });
        MockDate.reset();
    });
});
