import {
    getActData,
    getOrderData,
    getSaverData,
    getPrintFormData,
    getMemo,
    getStatus
} from '../act';

const data = {
    rows: [
        {
            consume: {
                seqnum: 0,
                order: {
                    text: 'Заказ 7-1475',
                    nds: 1,
                    id: 1098218928,
                    service: {
                        cc: 'PPC',
                        id: 7,
                        name: 'Директ: Рекламные кампании'
                    },
                    serviceOrderId: 40730438
                },
                unitName: 'у.е.',
                discountPct: '0.00'
            },
            price: '117.600000',
            amount: '0.01',
            manager: {},
            id: 618133731,
            quantity: '0.000094',
            finishSum: '0.01',
            actSum: '0.01'
        }
    ],
    factura: '20181211000009',
    amount: '0.01',
    invoice: {
        firm: {
            id: 25
        },
        postpay: false,
        paysys: {
            id: 2501021
        },
        currency: 'KZT',
        person: {
            id: 8968713,
            name: 'Физ. лицо, КазахстанYyK QQXSUG jxmPk'
        },
        client: {
            id: 105153935,
            name: 'createdClient_UVWqW'
        },
        id: 84545158,
        receiptSum1c: '0.00',
        totalSum: '5880.00',
        isoCurrency: 'KZT',
        externalId: 'Z-1262233603-1'
    },
    id: 87582042,
    hidden: false,
    dt: '2018-12-11T00:00:00',
    exportDt: '2018-12-11T17:38:58',
    isTrp: false,
    oebsExportable: true,
    isDocsDetailed: false,
    goodDebt: '0E-10',
    isDocsSeparated: false,
    actSum: '0.01',
    externalId: '146458304'
};

describe('data processors for act page', () => {
    it('getActData', () => {
        const expected = {
            dt: '2018-12-11T00:00:00',
            client: { id: 105153935, name: 'createdClient_UVWqW' },
            person: {
                id: 8968713,
                name: 'Физ. лицо, КазахстанYyK QQXSUG jxmPk'
            },
            invoice: { id: 84545158, externalId: 'Z-1262233603-1' },
            currency: 'KZT',
            totalSum: '5880.00',
            amount: '0.01',
            actSum: '0.01',
            receiptSum1c: '0.00'
        };

        expect(getActData(data)).toEqual(expected);
    });

    it('getOrderData', () => {
        const expected = [
            {
                actSum: '0.01',
                amount: '0.01',
                discountPct: '0.00',
                managerName: '',
                orderInfo: {
                    cc: 'PPC',
                    id: 40730438,
                    text: '7-40730438'
                },
                orderName: 'Заказ 7-1475',
                price: '117.600000',
                quantity: '0.000094',
                serviceName: 'Директ: Рекламные кампании',
                unitName: 'у.е.'
            }
        ];

        expect(getOrderData(data)).toEqual(expected);
    });

    it('getSaverData', () => {
        const expected = {
            id: 87582042,
            goodDebt: false,
            hidden: false,
            oebsExportable: true
        };

        expect(getSaverData(data)).toEqual(expected);
    });

    describe('getPrintFormData', () => {
        it('should generate invisible', () => {
            const expected = {
                id: 87582042,
                isVisible: false
            };

            expect(getPrintFormData(data)).toEqual(expected);
        });

        it('should generate some visible', () => {
            const { invoice } = data;
            const datas = [
                {
                    ...data,
                    invoice: { ...invoice, postpay: true, firm: { id: 4 } }
                },
                {
                    ...data,
                    invoice: { ...invoice, postpay: true, firm: { id: 7 } }
                },
                {
                    ...data,
                    invoice: { ...invoice, postpay: true, firm: { id: 8 } }
                },
                {
                    ...data,
                    invoice: { ...invoice, postpay: true, firm: { id: 14 } }
                },
                {
                    ...data,
                    invoice: { ...invoice, postpay: true, firm: { id: 16 } }
                },
                {
                    ...data,
                    isTrp: true,
                    invoice: { ...invoice, postpay: false, firm: { id: 1 } }
                },
                {
                    ...data,
                    isTrp: false,
                    invoice: { ...invoice, postpay: false, firm: { id: 4 } }
                },
                {
                    ...data,
                    isTrp: false,
                    invoice: { ...invoice, postpay: false, firm: { id: 8 } }
                }
            ];

            const expected = {
                id: 87582042,
                isVisible: true
            };

            datas.forEach(data => expect(getPrintFormData(data)).toEqual(expected));
        });
    });

    describe('getMemo', () => {
        it('should return string', () => {
            const memoData = { memo: 'memo' };

            expect(getMemo({ memo: JSON.stringify(memoData) })).toEqual(memoData.memo);
        });

        it('should return null', () => {
            const memoData = { memo: 'memo' };

            expect(getMemo(memoData)).toBe(null);
        });
    });

    it('getStatus', () => {
        const expected = {
            id: 87582042,
            externalId: '146458304',
            factura: '20181211000009',
            isDocsDetailed: false,
            isDocsSeparated: false,
            exportDt: '2018-12-11T17:38:58',
            hidden: false,
            oebsExportable: true
        };

        expect(getStatus(data)).toEqual(expected);
    });
});
