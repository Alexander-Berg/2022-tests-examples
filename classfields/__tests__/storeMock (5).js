module.exports = {
    user: { permissions: [ 'edit_client_placement_discounts' ] },
    client: {
        tariffs: {
            data: {
                tariff: 'minimum',
                plannedAction: {},
                callStats: {
                    total: 0,
                    success: 0,
                    target: 0,
                    nonTarget: 0,
                    missed: 0,
                    blocked: 0,
                    payedTuz: 0
                },
                priceLists: [],
                individualPriceLists: [],
                individualPricingOptions: {
                    from: '',
                    to: ''
                },
                individualPlacementDiscounts: [
                    {
                        id: 152,
                        clientId: '4047997043',
                        discount: 10,
                        from: '2020-06-30T21:00:00Z',
                        to: '2020-07-24T20:59:59Z',
                        createTime: '2020-07-30T08:43:24.478Z',
                        managerUid: '1120000000112888'
                    },
                    {
                        id: 154,
                        clientId: '4047997043',
                        discount: 5,
                        from: '2020-07-24T21:00:00Z',
                        to: '2020-07-31T20:59:59Z',
                        createTime: '2020-07-30T08:43:39.827Z',
                        managerUid: '1120000000112888'
                    }
                ],
                individualPlacementDiscountsOptions: {
                    from: '',
                    to: '',
                    discount: undefined
                },
                realty3: {
                    uid: '4047997043'
                }
            },
            network: {
                fetchClientTariffsStatus: 'loaded',
                disableExtendedTariffStatus: 'loaded',
                bindIndividualPriceListStatus: 'loaded',
                deleteIndividualPricingStatus: 'loaded',
                bindIndividualPlacementDiscountStatus: 'loaded',
                deleteIndividualPlacementDiscountStatus: 'loaded'
            }
        }
    }
};
