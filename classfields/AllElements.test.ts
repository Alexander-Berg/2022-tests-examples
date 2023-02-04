import type { Actions, VinReportComment } from '@vertis/schema-registry/ts-types/auto/api/vin/comments/comment_api_model';
import type { Commentable, CommentableItem, RawVinReport } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_report_model';
import type { YogaNode } from 'app/server/tmpl/YogaNode';
import { getPaidReportExample } from 'mocks/vinReport/paidReport';
import { toCamel } from 'snake-camel';
import { yogaToXml } from '../../../XMLBuilder';
import { CarSharing } from '../cells/Carsharing/Creator';
import { Constraints } from '../cells/Constraints/Creator';
import { Dtp } from '../cells/Dtp/Creator';
import { Insurance } from '../cells/Insurance/Creator';
import { InsurancePayments } from '../cells/InsurancePayments/Creator';
import { Leasings } from '../cells/Leasings/Creator';
import { Offers } from '../cells/Offers/Creator';
import { Owners } from '../cells/Owners/Creator';
import { Pledge } from '../cells/Pledge/Creator';
import { Pts } from '../cells/Pts/Creator';
import { Recalls } from '../cells/Recalls/Creator';
import { Repair } from '../cells/Repair/Creator';
import { Taxi } from '../cells/Taxi/Creator';
import { TechInspection } from '../cells/TechInspection/Creator';
import { TotalAuction } from '../cells/TotalAuction/Creator';
import { Photos } from '../cells/VehiclePhotos/Creator';
import { Wanted } from '../cells/Wanted/Creator';
import { nodesCreators } from '../helpers/SharedCode';
import { createHelpers } from '../Main';

function getPaidReportMock(): RawVinReport {
    return toCamel(getPaidReportExample()) as unknown as RawVinReport;
}

describe('cell', () => {
    const report = getPaidReportMock();
    const helpers = createHelpers(report);

    nodesCreators().forEach((creator) => {
        it(`${ creator.model.identifier }`, () => {
            const nodes = creator.cellNodes(report, helpers);
            const xml = yogaToXml(nodes);
            expect(xml).toMatchSnapshot();
        });
    });
});

describe('contents node', () => {
    const report = getPaidReportMock();
    const helpers = createHelpers(report);

    nodesCreators().forEach((creator) => {
        it(`${ creator.model.identifier }`, () => {
            const nodes: Array<YogaNode> = [];
            const node = creator.contentsNode(report, helpers);
            node && nodes.push(node);
            const xml = yogaToXml(nodes);
            expect(xml).toMatchSnapshot();
        });
    });
});

describe('preview', () => {
    const report = getPaidReportMock();
    const helpers = createHelpers(report);

    nodesCreators().forEach((creator) => {
        it(`${ creator.model.identifier }`, () => {
            const nodes: Array<YogaNode> = [];
            const node = creator.previewNode(report, helpers);
            node && nodes.push(node);
            const xml = yogaToXml(nodes);
            expect(xml).toMatchSnapshot();
        });
    });
});

type WithCommentsInfo = { commentsInfo?: CommentableItem }

describe('cell with comments', () => {
    const report = getPaidReportMock();
    const helpers = createHelpers(report);

    const commentable: Commentable = {
        blockId: '',
        addComment: true,
    };
    const actions: Actions = {
        edit: true,
        'delete': true,
    };
    const comment: VinReportComment = {
        id: '',
        blockId: '',
        text: 'Test Test Test Test Test Test Test Test Test Test Test Test ',
        photos: [],
        user: {
            id: 'string',
            name: 'Продавец',
            currentUserIsOwner: false,
        },
        createTime: '1618475193068',
        updateTime: '1622720682907',
        actions: actions,
        isDeleted: false,
        isExpired: false,
    };
    const comment1: VinReportComment = {
        id: '',
        blockId: '',
        text: 'Test Test Test Test Test Test Test Test Test Test Test Test ',
        photos: [],
        user: {
            id: 'string',
            name: 'Продавец',
            currentUserIsOwner: true,
        },
        createTime: '1618475193068',
        updateTime: '1622720682907',
        isDeleted: false,
        isExpired: false,
    };
    const commentsInfo: CommentableItem = {
        commentable: commentable,
        comments: [ comment, comment1 ],
    };

    function enrichWithCommentsInfo(object: WithCommentsInfo) {
        object.commentsInfo = commentsInfo;
    }

    function enrichArrayWithCommentsInfo(objects: Array<WithCommentsInfo>) {
        for (let index = 0; index < objects.length; index++) {
            objects[index].commentsInfo = commentsInfo;
        }
    }

    report.constraints && enrichWithCommentsInfo(report.constraints);
    report.leasings && enrichWithCommentsInfo(report.leasings);
    report.ptsOwners && enrichWithCommentsInfo(report.ptsOwners);
    report.pledge && enrichWithCommentsInfo(report.pledge);
    report.ptsInfo && enrichWithCommentsInfo(report.ptsInfo);
    report.wanted && enrichWithCommentsInfo(report.wanted);

    report.dtp?.items && enrichArrayWithCommentsInfo(report.dtp.items);
    report.insurances?.insurances && enrichArrayWithCommentsInfo(report.insurances.insurances);
    report.insurancePayments?.payments && enrichArrayWithCommentsInfo(report.insurancePayments.payments);
    report.autoruOffers?.offers && enrichArrayWithCommentsInfo(report.autoruOffers.offers);
    report.recalls?.recallRecords && enrichArrayWithCommentsInfo(report.recalls.recallRecords);
    report.repairCalculations?.calculationRecords && enrichArrayWithCommentsInfo(report.repairCalculations.calculationRecords);
    report.taxi?.taxiRecords && enrichArrayWithCommentsInfo(report.taxi.taxiRecords);
    report.techInspectionBlock?.records && enrichArrayWithCommentsInfo(report.techInspectionBlock.records);
    report.totalAuction?.totalAuctionRecords && enrichArrayWithCommentsInfo(report.totalAuction.totalAuctionRecords);
    report.vehiclePhotos?.records && enrichArrayWithCommentsInfo(report.vehiclePhotos.records);

    const commentableCreators = [
        new Owners(),
        new TechInspection(),
        new TotalAuction(),
        new Repair(),
        new Taxi(),
        new Leasings(),
        new InsurancePayments(),
        new Dtp(),
        new CarSharing(),
        new Recalls(),
        new Photos(),
        new Insurance(),
        new Offers(),
        new Pledge(),
        new Wanted(),
        new Constraints(),
        new Pts(),
    ];

    commentableCreators.forEach((creator) => {
        it(`${ creator.model.identifier }`, () => {
            const nodes = creator.cellNodes(report, helpers);
            const xml = yogaToXml(nodes);
            expect(xml).toMatchSnapshot();
        });
    });
});
