module.exports = {
    user: { permissions: [ 'edit_client_individual_price_lists' ] },
    client: {
        tariffs: {
            data: {
                callStats: {},
                turnedOn: true,
                priceLists: [
                    {
                        name: '50-percent-discount',
                        title: '50% скидка'
                    },
                    {
                        name: '10-rubley',
                        title: '10 рублей'
                    }
                ],
                individualPriceLists: [
                    {
                        clientId: '4033993652',
                        createTime: '2019-12-24T00:58:51Z',
                        from: '2019-12-23T00:00:00Z',
                        id: 111,
                        managerUid: '1120000000112888',
                        priceList: {
                            id: 48,
                            name: '50-percent-discount',
                            title: '50% скидка'
                        },
                        to: '2019-12-29T00:00:00Z'
                    },
                    {
                        clientId: '4033993652',
                        createTime: '2019-12-24T00:58:51Z',
                        from: '2019-10-23T00:00:00Z',
                        id: 116,
                        managerUid: '1120000000112888',
                        priceList: {
                            id: 48,
                            name: '10-rubley',
                            title: '10 рублей'
                        },
                        to: '2019-12-10T00:00:00Z'
                    }
                ],
                individualPricingOptions: {
                    from: '',
                    to: '',
                    priceListName: undefined
                },
                individualPlacementDiscounts: [],
                individualPlacementDiscountsOptions: {
                    from: '',
                    to: '',
                    discount: undefined
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
