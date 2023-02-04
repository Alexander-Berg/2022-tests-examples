const spent = {
    timestamp: '2021-10-26T00:00:00.000+03:00',
    summary: [
        {
            product: 'RAISE',
            amount: '4000',
        },
        {
            product: 'PLACEMENT',
            amount: '34000',
        },
        {
            product: 'PAYED_TUZ_CALL',
            amount: '39000',
        },
        {
            product: 'PROFILE_CALL',
            amount: '44000',
        },
        {
            product: 'PAID_REPORT',
            amount: '12300',
        },
    ],
};

const billingEventsList = {
    '2021-10-26T00:00:00.000+03:00': [
        {
            product: 'RAISE',
            offerInfo: {
                offers: [
                    {
                        offerId: '8943067761507192998',
                        amount: 2500,
                    },
                    {
                        offerId: '7429161182156671631',
                        amount: 1500,
                    },
                ],
                calls: [],
                paidReports: [],
            },
        },
        {
            product: 'PLACEMENT',
            offerInfo: {
                offers: [
                    {
                        offerId: '533939526965050987',
                        amount: 17000,
                    },
                    {
                        offerId: '1894618737792967061',
                        amount: 17000,
                    },
                ],
            },
        },
        {
            product: 'PAYED_TUZ_CALL',
            callInfo: {
                calls: [
                    {
                        callDetails: {
                            callId: '4729341422415363280',
                            offerCategory: 'APARTMENT',
                            offerType: 'SELL',
                        },
                        amount: 190000,
                    },
                    {
                        callDetails: {
                            callId: '4729341422415363280',
                            offerCategory: 'APARTMENT',
                            offerType: 'SELL',
                        },
                        amount: 20000,
                    },
                ],
            },
        },
        {
            product: 'PROFILE_CALL',
            callInfo: {
                calls: [
                    {
                        callDetails: {
                            callId: '4729341422415363280',
                            offerCategory: 'APARTMENT',
                            offerType: 'SELL',
                        },
                        amount: 240000,
                    },
                    {
                        callDetails: {
                            callId: '4729341422415363280',
                            offerCategory: 'APARTMENT',
                            offerType: 'SELL',
                        },
                        amount: 20000,
                    },
                ],
            },
        },
        {
            product: 'PAID_REPORT',
            paidReportInfo: {
                paidReports: [
                    {
                        paidReportDetails: {
                            paidReportId: '6aedbd67627847ec95a4b6d56eef8db8',
                            offerId: '4729341422415363280',
                        },
                        amount: 12300,
                    },
                ],
            },
        },
    ],
};

export const store = {
    config: {
        timeDelta: 0,
    },
    periodFilters: {
        startTime: '2021-10-25T21:00:00.000Z',
        endTime: '2021-10-26T20:59:59.999Z',
    },
    finances: {
        aggregatedSpentSummaryDatesRange: ['2021-10-25T21:00:00.000Z', '2021-10-26T20:59:59.999Z'],
        status: 'loaded',
        aggregatedSpentSummaryByDays: [spent],
        aggregatedSpentSummary: {
            timestamp: '2021-10-26T00:00:00.000+03:00',
            summary: spent.summary,
        },
        billingEventsList,
    },
};
