import type { RawVinReport } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { response200Paid } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import { createHelpers, makeOfferPreviewNode } from 'app/server/tmpl/android/v1/Main';
import type { YogaNode } from 'app/server/tmpl/YogaNode';
import type { Request } from 'express';
import { last } from 'lodash';
import { YaImagesModel } from './Model';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
});

describe('carfax preview', () => {
    it('block not exists', () => {
        const report = response200Paid().report as RawVinReport;
        report.status = Status.OK;
        report.ya_images = undefined;
        const result = render(report, req);
        const yandexImagesRegex = /Фотографии из Яндекс Картинок/;
        expect(allChildren(result).map((item) => item.text).filter(item => item && yandexImagesRegex.test(item))).toEqual([]);
    });
    it('should extract title for preview', () => {
        const report = response200Paid().report as RawVinReport;
        report.status = Status.OK;
        report.ya_images = {
            header: {
                title: 'Фотографии из Яндекс Картинок',
                is_updating: false,
                timestamp_update: '',
            },
            status: Status.OK,
            record_count: 1,
            ya_images_records: [
                {
                    external_uri: 'https://preview.redd.it/2l2av8at5sn31.jpg',
                    size: '200x200',
                    title: 'Reddit.com',
                },
            ],
        };
        const model = new YaImagesModel();
        const result = model.previewModel(report);
        expect(result).not.toBeUndefined();
        expect(result!.id).toBe(model.identifier);
        expect(result!).toMatchSnapshot();
    });
    it('should render one preview', () => {
        const report = response200Paid().report as RawVinReport;
        report.status = Status.OK;
        report.ya_images = {
            header: {
                title: 'Фотографии из Яндекс Картинок',
                is_updating: false,
                timestamp_update: '',
            },
            status: Status.OK,
            record_count: 1,
            ya_images_records: [
                {
                    external_uri: 'https://preview.redd.it/2l2av8at5sn31.jpg',
                    size: '200x200',
                    title: 'Reddit.com',
                },
            ],
        };
        const result = render(report, req);
        const items = last(result.children?.[1]?.children?.[1]?.children);
        /* eslint-disable jest/no-truthy-falsy */
        expect(items).toBeTruthy();
        /* eslint-disable @typescript-eslint/ban-ts-comment */
        // @ts-ignore
        expect(items?.children[0]).toMatchSnapshot();
    });
});

function allChildren(node: YogaNode): Array<YogaNode> {
    return [ node, ...(node.children?.flatMap((x) => allChildren(x)) ?? []) ];
}

function render(report: RawVinReport, req: Request): YogaNode {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeOfferPreviewNode(report, helpers);
}
