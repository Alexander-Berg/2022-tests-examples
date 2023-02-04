import { createStateFromQuery, TargetType } from '../create-state-from-query';
import { SortOrder } from '../../constants';

describe('test for createStateFromQuery', () => {
    beforeEach(() => {
        window.history.pushState(
            {},
            'Test Title',
            `/page.xml?type=&client_id=110519488&payment_status=4&service_cc=bug_bounty&contract_id=1152837&service_order_id=&issue_dt_from=2019-11-01T00%3A00%3A00&issue_dt_to=2019-11-14T00%3A00%3A00&ps=10&sf=issue_dt&so=0#`
        );
    });

    it('try get state from query', () => {
        const fromQSToState = createStateFromQuery({
            client_id: ['next.agency.id', TargetType.NUMBER],
            payment_status: ['next.paymentStatus', TargetType.NUMBER],
            service_cc: ['next.serviceCc', TargetType.STRING],
            type: ['next.type', TargetType.STRING],
            contract_id: ['next.contractId', TargetType.NUMBER],
            service_order_id: ['next.serviceOrderId', TargetType.NUMBER],
            issue_dt_from: ['next.dateFrom', TargetType.DATE],
            issue_dt_to: ['next.dateTo', TargetType.DATE],
            ps: ['next.pageSize', TargetType.NUMBER],
            pn: ['next.pageNumber', TargetType.NUMBER],
            sf: ['next.sort.sortBy', TargetType.CUSTOM, x => x],
            so: [
                'next.sort.sortOrder',
                TargetType.CUSTOM,
                x => (x === '0' ? SortOrder.DESC : SortOrder.ASK)
            ]
        });

        expect(fromQSToState()).toEqual({
            next: {
                agency: { id: 110519488 },
                contractId: 1152837,
                dateFrom: '2019-11-01T00:00:00',
                dateTo: '2019-11-14T00:00:00',
                pageSize: 10,
                paymentStatus: 4,
                serviceCc: 'bug_bounty',
                sort: {
                    sortBy: 'issue_dt',
                    sortOrder: 'DESC'
                }
            }
        });
    });
});
