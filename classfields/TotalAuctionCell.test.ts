import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, TotalAuctionBlock, TotalAuctionItem } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('total auction cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.total_auction = undefined;

        const result = render(report, req);
        const totalAuction = result.find((node) => node.id === 'total_auction');

        expect(totalAuction).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.total_auction = buildTotalAuctionItem(true, true);

        const result = render(report, req);
        const totalAuction = result.find((node) => node.id === 'total_auction');

        expect(get(totalAuction, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.total_auction = buildTotalAuctionItem(false, true);

        const result = render(report, req);
        const totalAuction = result.find((node) => node.id === 'total_auction');

        expect(get(totalAuction, 'children[1].children[0]].children[0]].children[1].text'))
            .toContain('Автомобиль не продавался на аукционах битых автомобилей');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.total_auction = buildTotalAuctionItem(false, false);

        const result = render(report, req);
        const totalAuction = result.find((node) => node.id === 'total_auction');
        const xml = totalAuction ? yogaToXml([ totalAuction ]) : '';

        expect(totalAuction).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildTotalAuctionItem(isUpdating: boolean, isEmpty: boolean): TotalAuctionBlock {
    let records: Array<TotalAuctionItem> = [];
    if (!isEmpty) {
        records = [
            {
                date: '1555077600000',
                auction: 'Migtorg',
                region: 'Россия',
                gallery: [
                    {
                        name: 'autoru-carfax:2407706-wVC9WvefjHcmRwguAk5iRQO861dkkqJUi',
                        sizes: {
                            thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2407706/wVC9WvefjHcmRwguAk5iRQO861dkkqJUi/thumb_m',
                        },
                    },
                    {
                        name: 'autoru-carfax:2788728-guKENwcoGcfzj5SblxVUPLGNuFvO2xDv5',
                        sizes: {
                            thumb_m: '//images.mds-proxy.test.avto.ru/get-autoru-carfax/2788728/guKENwcoGcfzj5SblxVUPLGNuFvO2xDv5/thumb_m',
                        },
                    },
                ],
                comments_info: {
                    commentable: {
                        block_id: 'total_auction:1555077600000',
                        add_comment: false,
                    },
                },
            } as unknown as TotalAuctionItem,
        ];
    }
    return {
        header: {
            title: isEmpty ? undefined : 'Продавался на аукционах битых автомобилей',
            timestamp_update: '1644478739424',
            is_updating: isUpdating,
        },
        total_auction_records: records,
        status: isEmpty ? undefined : Status.ERROR,
        record_count: 1,
    } as unknown as TotalAuctionBlock;
}
