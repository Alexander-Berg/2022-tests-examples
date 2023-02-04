import type { VinReportContentItem } from '../models';

import get_vin_report_content from './get_vin_report_content';

it('содержание отчЁта если записей 3 и меньше', () => {
    const report_content = [
        { key: '1', type: 'DTP' as VinReportContentItem['type'], record_count: -1 },
        { key: '2', type: 'AUTORU_OFFERS' as VinReportContentItem['type'], record_count: 1 },
        { key: '3', type: 'MILEAGES_GRAPH' as VinReportContentItem['type'], record_count: 5 },
    ];

    expect(get_vin_report_content(report_content)).toMatchSnapshot();
});

it('содержание отчЁта если записей больше 3', () => {
    const report_content = [
        { key: '1', type: 'AUTORU_OFFERS' as VinReportContentItem['type'], record_count: 5 },
        { key: '2', type: 'HISTORY' as VinReportContentItem['type'], record_count: 1 },
        { key: '3', type: 'TAXI' as VinReportContentItem['type'] },
        { key: '4', type: 'DTP' as VinReportContentItem['type'], record_count: 3 },
    ];

    expect(get_vin_report_content(report_content)).toMatchSnapshot();
});
