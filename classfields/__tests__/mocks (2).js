module.exports = {
    getDefaultGeoTextOpts() {
        return {
            isLocative: true,
            refinements: {
                subLocality: {
                    list: []
                },
                direction: {
                    list: []
                },
                street: {
                    list: []
                },
                siteId: {
                    list: []
                }
            },
            searchParams: {
                rgid: 587795,
                currency: 'RUR',
                yandexUid: '9999999999999999999',
                pageSize: 20,
                subLocality: '12446',
                sort: 'RELEVANCE',
                type: 'SELL',
                category: 'APARTMENT',
                maxCoordinates: 500,
                showUniquePoints: 'NO',
                searchType: 'search',
                from: 'direct',
                login: 'yandexuid:9999999999999999999',
                showRevoked: 'YES',
                expFlags: [ ]
            },
            geo: {
                id: 213,
                rgid: 587795,
                type: 'CITY',
                name: 'Москва',
                locative: 'в Москве',
                populatedRgid: 587795,
                parents: [
                    {
                        id: 1,
                        rgid: '741964',
                        name: 'Москва и МО',
                        type: 'SUBJECT_FEDERATION'
                    },
                    {
                        id: 225,
                        rgid: '143',
                        name: 'Россия',
                        type: 'COUNTRY',
                        genitive: 'России'
                    },
                    {
                        id: 0,
                        rgid: '0',
                        name: 'Весь мир',
                        type: 'UNKNOWN'
                    }
                ]
            }
        };
    },
    getDefaultCategoryOpts() {
        return {
            searchParams: {
                type: 'SELL',
                category: 'APARTMENT'
            }
        };
    }
};
