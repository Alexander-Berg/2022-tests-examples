import { ReportType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';

import isReportNeedPay from './isReportNeedPay';

const PTS_OWNERS = {
    header: { title: 'Владельцы по ПТС', updated: '43252352352365235' },
    data: {},
};

const PTS_INFO = {
    header: { title: 'Данные по ПТС', updated: '3029429839238724' },
    commentable: {
        add_comment: true,
    },
};

const DTP = {
    items: [ {
        commantable: {
            add_comment: false,
        },
    } ],
};

let VIN_REPORT: StateVinReportData;
beforeEach(() => {
    VIN_REPORT = {
        report: {
            pts_owners: PTS_OWNERS,
            pts_info: PTS_INFO,
            dtp: DTP,
            report_type: 'FREE_REPORT',
        },
        billing: {
            service_prices: [
                {
                    counter: '1',
                    price: 0,
                },
            ],
        },
    } as unknown as StateVinReportData;
});

it('isReportNeedPay должен вернуть `true`, если цена 0 и тип FREE_REPORT', () => {
    const result = isReportNeedPay(VIN_REPORT);
    expect(result).toEqual(true);
});

it('isReportNeedPay должен вернуть `true`, если цена не 0 и тип FREE_REPORT', () => {
    VIN_REPORT.billing!.service_prices[0].price = 100;
    const result = isReportNeedPay(VIN_REPORT);
    expect(result).toEqual(true);
});

it('isReportNeedPay должен вернуть `false`, если тип не FREE_REPORT', () => {
    VIN_REPORT.report!.report_type = ReportType.UNKNOWN_REPORT_TYPE;
    const result = isReportNeedPay(VIN_REPORT);
    expect(result).toEqual(false);
});
