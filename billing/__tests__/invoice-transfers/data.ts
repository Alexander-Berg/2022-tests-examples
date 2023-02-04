import { HOST } from 'common/utils/test-utils/common';
import { mocks as pageMocks } from '../page.data';

export const mocks = {
    invoiceTransfers: {
        request: {
            url: `${HOST}/invoice/invoice-transfers`,
            data: {
                invoice_id: '123',
                pagination_pn: 1,
                pagination_ps: 10,
                sort_key: 'DT',
                sort_order: 'ASC'
            }
        },
        response: {
            totalCount: 2,
            items: [
                {
                    id: 111,
                    srcInvoice: {
                        id: 123,
                        externalId: 'Б-123'
                    },
                    dstInvoice: {
                        id: 456,
                        externalId: 'Б-456'
                    },
                    amout: '123.45',
                    status: 'in_progress',
                    dt: '2021-11-11T00:00:00',
                    updateDt: '2021-11-12T00:00:00',
                    unlockAllowed: false
                },
                {
                    id: 112,
                    srcInvoice: {
                        id: 123,
                        externalId: 'Б-123'
                    },
                    dstInvoice: {
                        id: 456,
                        externalId: 'Б-456'
                    },
                    amout: '123.45',
                    status: 'successful',
                    dt: '2021-11-11T00:00:00',
                    updateDt: '2021-11-12T00:00:00',
                    unlockAllowed: true
                }
            ]
        }
    },
    emptyInvoiceTransfers: {
        request: {
            url: `${HOST}/invoice/invoice-transfers/list`,
            data: {
                ctype: 'GENERAL',
                pn: 1,
                ps: 10
            }
        },
        response: {
            totalCount: 0,
            items: []
        }
    },
    invoiceWithoutMoney: {
        ...pageMocks.invoice,
        response: {
            ...pageMocks.invoice.response,
            data: {
                ...pageMocks.invoice.response.data,
                'available-invoice-transfer-sum': '0'
            }
        }
    },
    unlockInvoiceTransfer: {
        request: {
            url: `${HOST}/invoice/unlock-invoice-transfer`,
            data: {
                invoice_transfer_id: 112
            }
        },
        response: {
            availableInvoiceTransferSum: '9999999',
            status: 'NEW'
        }
    },
    failedToUnlockInvoiceTransfer: {
        request: {
            url: `${HOST}/invoice/unlock-invoice-transfer`,
            data: {
                invoice_transfer_id: 112
            }
        },
        error: {
            data: {
                error: 'SOME_ERROR',
                description: 'some error occured'
            }
        }
    },
    createInvoiceTransferWithAllMoney: {
        request: {
            url: `${HOST}/invoice/create-invoice-transfer`,
            data: {
                src_invoice_external_id: 'Б-3561148159-1',
                dst_invoice_external_id: '234',
                all_money: true,
                available_invoice_transfer_sum: '850000.00'
            }
        },
        response: {
            availableInvoiceTransferSum: '0.00',
            invoiceTransfer: {
                id: 112,
                srcInvoice: {
                    id: 123,
                    externalId: 'Б-123'
                },
                dstInvoice: {
                    id: 456,
                    externalId: 'Б-456'
                },
                amout: '123.45',
                status: 'successful',
                dt: '2021-11-11T00:00:00',
                updateDt: '2021-11-12T00:00:00',
                unlockAllowed: true
            }
        }
    },
    createInvoiceTransferWithSomeMoney: {
        request: {
            url: `${HOST}/invoice/create-invoice-transfer`,
            data: {
                src_invoice_external_id: 'Б-3561148159-1',
                dst_invoice_external_id: '234',
                amount: '777',
                available_invoice_transfer_sum: '850000.00'
            }
        },
        response: {
            availableInvoiceTransferSum: '77777',
            invoiceTransfer: {
                id: 112,
                srcInvoice: {
                    id: 123,
                    externalId: 'Б-123'
                },
                dstInvoice: {
                    id: 456,
                    externalId: 'Б-456'
                },
                amout: '123.45',
                status: 'successful',
                dt: '2021-11-11T00:00:00',
                updateDt: '2021-11-12T00:00:00',
                unlockAllowed: true
            }
        }
    },
    failedTocreateInvoiceTransfer: {
        request: {
            url: `${HOST}/invoice/create-invoice-transfer`,
            data: {
                src_invoice_external_id: 'Б-3561148159-1',
                dst_invoice_external_id: '234',
                all_money: true,
                available_invoice_transfer_sum: '850000.00'
            }
        },
        error: {
            data: {
                error: 'SOME_ERROR',
                description: 'some error occured'
            }
        }
    }
};
