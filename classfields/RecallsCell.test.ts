import { createHelpers, makeFullReport } from 'app/server/tmpl/android/v1/Main';
import { AppConfig } from 'app/server/tmpl/android/v1/helpers/AppConfig';
import type { Request } from 'express';
import type {
    CommentableItem,
    RawVinReport,
    RecallRecord,
    RecallRecord_Recall,
    RecallsBlock,
} from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { response200ForVin } from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { yogaToXml } from 'app/server/tmpl/XMLBuilder';
import { get } from 'lodash';

let req: Request;
beforeEach(() => {
    req = {
        headers: {
            'x-android-app-version': '10.0.0',
        },
    } as unknown as Request;
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

describe('recalls cell', () => {
    it('block not exists', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.recalls = undefined;

        const result = render(report, req);
        const insurance = result.find((node) => node.id === 'recalls');

        expect(insurance).toBeUndefined();
    });

    it('block not ready', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.recalls = buildRecallsItem(true, true);

        const result = render(report, req);
        const recalls = result.find((node) => node.id === 'recalls');

        expect(get(recalls, 'children[1].children[0].url')).toContain('content-loading-1');
    });

    it('block exist', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.recalls = buildRecallsItem(false, false);

        const result = render(report, req);
        const recalls = result.find((node) => node.id === 'recalls');
        const xml = recalls ? yogaToXml([ recalls ]) : '';

        expect(recalls).toMatchSnapshot();
        expect(xml).toMatchSnapshot();
    });

    it('block empty', () => {
        const report = response200ForVin('Z8T4DNFUCDM014995').report as RawVinReport;
        report.recalls = buildRecallsItem(false, true);

        const result = render(report, req);
        const recalls = result.find((node) => node.id === 'recalls');

        expect(get(recalls, 'children[1].children[0].children[0].children[1].text'))
            .toContain('Отзывные кампании не обнаружены');
    });
});

function render(report: RawVinReport, req: Request) {
    const appConfig = new AppConfig({}, req);
    const helpers = createHelpers(report, appConfig);
    return makeFullReport(report, helpers);
}

function buildRecallsItem(isUpdating: boolean, isEmpty: boolean): RecallsBlock {
    const recallRecords: Array<RecallRecord> = [];
    if (!isEmpty) {
        recallRecords.push(
            {
                recall: {
                    recall_id: '1816',
                    campaign_id: '425',
                    title: 'Обжимаемый участок фитинга тормозного шланга имеет недостаточное уплотнение',
                    // eslint-disable-next-line max-len
                    description: 'Причиной отзыва транспортных средств является то, что в результате недостаточной оценки долговечности тормозного шланга под действием давления тормозной жидкости на некоторых автомобилях обжимаемый участок фитинга шланга имеет недостаточное уплотнение. В результате этого тормозная жидкость может выдавливаться между внутренним и внешним слоями шланга, приводя к его вздутию.',
                    // eslint-disable-next-line max-len
                    url: 'https://www.rst.gov.ru/portal/gost/home/presscenter/news?portal:componentId=88beae40-0e16-414c-b176-d0ab5de82e16&navigationalstate=JBPNS_rO0ABXczAAZhY3Rpb24AAAABAA5zaW5nbGVOZXdzVmlldwACaWQAAAABAAQ2NTI2AAdfX0VPRl9f',
                    published_timestamp: '1575968400000',
                    is_resolved: true,
                } as RecallRecord_Recall,
                comments_info: {
                    commentable: {
                        block_id: 'recall:425',
                        add_comment: false,
                    },
                } as CommentableItem,
            },
            {
                recall: {
                    recall_id: '1809',
                    campaign_id: '418',
                    title: 'Ошибка в программировании блока управления двигателем (ECM)',
                    // eslint-disable-next-line max-len
                    description: 'Причиной отзыва транспортных средств Subaru является то, что  в результате ошибки в программировании блока управления двигателем (ECM), в определенных обстоятельствах напряжение на катушке зажигания может сохраняться после выключения двигателя дольше, чем это предусмотрено. В результате длительного сохранения напряжения на катушке зажигания ее внутренняя температура может возрастать, что в свою очередь может привести к короткому замыканию. Более того, на автомобилях, оснащенных определенными жгутами проводов и модулями зажигания, которые были произведены исключительно для затронутых автомобилей, возможно перегорание предохранителя, что приводит к остановке двигателя и невозможности его повторного запуска.',
                    // eslint-disable-next-line max-len
                    url: 'https://www.rst.gov.ru/portal/gost/home/presscenter/news?portal:isSecure=true&navigationalstate=JBPNS_rO0ABXczAAZhY3Rpb24AAAABAA5zaW5nbGVOZXdzVmlldwACaWQAAAABAAQ2NDcxAAdfX0VPRl9f&portal:componentId=88beae40-0e16-414c-b176-d0ab5de82e16',
                    published_timestamp: '1574690400000',
                    is_resolved: false,
                } as RecallRecord_Recall,
                comments_info: {
                    commentable: {
                        block_id: 'recall:418',
                        add_comment: false,
                    },
                } as CommentableItem,
            },
            {
                recall: {
                    recall_id: '2485',
                    campaign_id: '1089',
                    title: 'Болты кронштейна заднего стабилизатора',
                    // eslint-disable-next-line max-len
                    description: 'Во время эксплуатации автомобиля болты кронштейна заднего стабилизатора могут ослабевать, и это может стать причиной аномального шума. Если автомобиль продолжает эксплуатироваться, то болт может выпасть, а сам кронштейн может начать свободно двигаться во время движения. В худшем случае он может повредить окружающие детали, такие, как тормозная трубка, ведущий вал или подрамник\nНа всех транспортных средствах будет проверено состояние болтов и проведена их затяжка до необходимого момента. В случае утери болта/болтов, будут установлены новые болты.',
                    url: 'https://www.rst.gov.ru//newsRST/redirect/REVOCABLE_PROGRAMS/95/8085',
                    published_timestamp: '1621969200000',
                } as RecallRecord_Recall,
                comments_info: {
                    commentable: {
                        block_id: 'recall:1089',
                        add_comment: false,
                    },
                } as CommentableItem,
            },
        );
    }
    return {
        header: {
            title: 'Проверка отзывных кампаний',
            timestamp_update: '1644306310777',
            is_updating: isUpdating,
        },
        recall_records: recallRecords,
        status: Status.OK,
        record_count: 3,
    } as unknown as RecallsBlock;
}
