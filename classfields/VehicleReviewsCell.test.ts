import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type { RawVinReport, ReviewItem, ReviewsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';
import type { Photo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
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

describe('vehicle reviews cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reviews = undefined;

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'vehicle_reviews');

        expect(owners).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reviews = buildVehicleReviewsItem(true, true);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'vehicle_reviews');

        expect(get(owners, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reviews = buildVehicleReviewsItem(false, true);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'vehicle_reviews');

        expect(owners).toBeUndefined();
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.reviews = buildVehicleReviewsItem(false, false);

        const result = render(report, req);
        const owners = result.find((node) => node.id === 'vehicle_reviews');
        const xml = owners ? yogaToXml([ owners ]) : '';

        expect(owners).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildVehicleReviewsItem(isUpdating: boolean, isEmpty: boolean): ReviewsBlock {
    let reviews: Array<ReviewItem> = [];

    if (!isEmpty) {
        reviews = [
            {
                id: '123',
                title: 'Отзыв номер 1',
                published_timestamp: '1621256899016',
                rating: 1.2,
                text: 'Описание отзыва: отзыв с одной фоткой',
                url: 'https://media.auto.ru/review/cars/vaz/2103/6257051/4469251119274136739/',
                photos: [
                    {
                        name: 'autoru-vos:3909203-67e8975bda8066d0e608b05b0be4367f',
                        sizes: {
                            thumb_m: '//avatars.mds.yandex.net/get-autoru-vos/3909203/67e8975bda8066d0e608b05b0be4367f/thumb_m',
                        },
                        namespace: 'autoru-vos',
                    } as unknown as Photo,
                ],
            },
            {
                id: '234',
                title: 'Отзыв номер 2',
                published_timestamp: '1621256999016',
                rating: 3.3,
                text: 'Описание отзыва: отзыв с тремя фотками',
                photos: [
                    {
                        name: 'autoru-vos:3909203-67e8975bda8066d0e608b05b0be43671',
                        sizes: {
                            thumb_m: '//avatars.mds.yandex.net/get-autoru-vos/3909203/67e8975bda8066d0e608b05b0be43671/thumb_m',
                        },
                        namespace: 'autoru-vos',
                    } as unknown as Photo,
                    {
                        name: 'autoru-vos:3909203-67e8975bda8066d0e608b05b0be43672',
                        sizes: {
                            thumb_m: '//avatars.mds.yandex.net/get-autoru-vos/3909203/67e8975bda8066d0e608b05b0be43672/thumb_m',
                        },
                        namespace: 'autoru-vos',
                    } as unknown as Photo,
                    {
                        name: 'autoru-vos:3909203-67e8975bda8066d0e608b05b0be43673',
                        sizes: {
                            thumb_m: '//avatars.mds.yandex.net/get-autoru-vos/3909203/67e8975bda8066d0e608b05b0be43673/thumb_m',
                        },
                        namespace: 'autoru-vos',
                    } as unknown as Photo,
                ],
            } as ReviewItem,
            {
                id: '345',
                title: 'Отзыв номер 2',
                published_timestamp: '1621156899016',
                rating: 4.7,
                text: 'Описание отзыва: отзыв без фоток',
                url: 'https://media.auto.ru/review/cars/vaz/2103/6257051/4469251119274136739/',
                photos: [],
            },
        ];
    }
    return {
        header: {
            title: 'Фотографии из отзывов владельцев',
            timestamp_update: '1621252899016',
            is_updating: isUpdating,
        },
        reviews_records: reviews,
        status: Status.OK,
        record_count: isEmpty ? 0 : 3,
    } as ReviewsBlock;
}
