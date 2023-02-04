/* eslint-disable */
import merge from 'lodash/merge';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

export const getOffers = additional => {
    return {
        'status === banned': getOffer({
            status: 'banned',
            banReasons: [],
            ...additional
        }),
        'status === "inactive"': getOffer({
            status: 'inactive',
            banReasons: [],
            ...additional
        }),
        'status === "inactive", banReasons': getOffer({
            status: 'inactive',
            banReasons: [
                {
                    code: 1,
                    title: 'Ошибка в адресе или координатах'
                }
            ],
            ...additional
        }),
        'status === "active"': getOffer({
            status: 'active',
            banReasons: [],
            ...additional
        }),
        'status === "moderation"': getOffer({
            status: 'moderation',
            banReasons: [],
            ...additional
        }),
        'status === "draft"': getDraft()
    };
};

export const getState = (additionalParams = {}) => {
    return merge({}, {
        config: {
            timeDelta: -1961
        },
        offersNew: {
            batchStatus: {
                status: '',
                offerIds: []
            }
        },
        vosUserData: {
            paymentType: 'NATURAL_PERSON',
            userType: 'OWNER',
            extendedUserType: 'OWNER'
        },
        renewalProblems: {
            notEnoughFundsForFutureRenewals: false
        },
        user: {
            isJuridical: false,
            crc: '123'
        }
    }, additionalParams);
};

export const getDraft = (additionalParams = {}) => {
    return merge({}, {
            id: '5409948112816228096',
            status: 'draft',
            banReasons: [],
            isEditable: true,
            stats: {
                statsPeriod: 30,
                cardShow: {
                    status: 'success',
                    offerId: '5409948112816228096',
                    details: [
                        {
                            timestamp: '2020-07-05T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-06T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-07T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-08T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-09T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-10T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-11T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-12T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-13T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-14T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-15T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-16T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-17T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-18T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-19T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-20T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-21T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-22T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-23T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-24T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-25T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-26T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-27T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-28T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-29T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-30T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-31T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-01T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-02T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-03T00:00:00.000+03:00',
                            value: 0
                        }
                    ],
                    total: 0
                },
                offerShow: {
                    status: 'success',
                    offerId: '5409948112816228096',
                    details: [
                        {
                            timestamp: '2020-07-05T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-06T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-07T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-08T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-09T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-10T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-11T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-12T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-13T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-14T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-15T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-16T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-17T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-18T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-19T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-20T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-21T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-22T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-23T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-24T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-25T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-26T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-27T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-28T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-29T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-30T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-31T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-01T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-02T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-03T00:00:00.000+03:00',
                            value: 0
                        }
                    ],
                    total: 0
                },
                phoneShow: {
                    status: 'success',
                    offerId: '5409948112816228096',
                    details: [
                        {
                            timestamp: '2020-07-05T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-06T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-07T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-08T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-09T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-10T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-11T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-12T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-13T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-14T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-15T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-16T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-17T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-18T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-19T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-20T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-21T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-22T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-23T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-24T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-25T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-26T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-27T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-28T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-29T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-30T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-07-31T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-01T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-02T00:00:00.000+03:00',
                            value: 0
                        },
                        {
                            timestamp: '2020-08-03T00:00:00.000+03:00',
                            value: 0
                        }
                    ],
                    total: 0
                },
                calls: {
                    status: 'success',
                    loading: false,
                    total: 0,
                    details: [
                        {
                            timestamp: '2020-07-04T21:00:00Z',
                            value: 0
                        }
                    ]
                },
                aggregated: [
                    {
                        timestamp: '2020-07-05T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-06T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-07T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-08T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-09T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-10T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-11T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-12T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-13T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-14T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-15T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-16T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-17T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-18T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-19T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-20T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-21T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-22T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-23T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-24T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-25T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-26T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-27T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-28T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-29T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-30T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-07-31T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-08-01T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-08-02T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    },
                    {
                        timestamp: '2020-08-03T00:00:00.000+03:00',
                        value: {
                            offerShow: 0,
                            cardShow: 0,
                            phoneShow: 0
                        }
                    }
                ]
            },
            offerType: 'SELL',
            category: 'APARTMENT',
            currency: 'RUB',
            address: 'улица Комиссара Смирнова',
            location: {
                rgid: 741965,
                country: 'Россия',
                address: 'улица Комиссара Смирнова',
                latitude: 59.96237183,
                longitude: 30.34997177,
                point: {
                    longitude: 30.34997177,
                    latitude: 59.96237183
                }
            },
            unifiedLocation: {
                metro: []
            },
            photos: [],
            createTime: '2020-08-03T13:13:25.000Z',
            updateTime: '2020-08-03T13:13:37.000Z',
            endOfShow: '2020-08-10T13:13:37.000Z',
            showDuration: 7,
            services: {
                premium: {
                    status: 'inactive',
                    renewal: {
                        status: 'INACTIVE'
                    }
                },
                raising: {
                    status: 'inactive',
                    renewal: {
                        status: 'INACTIVE'
                    }
                },
                promotion: {
                    status: 'inactive',
                    renewal: {
                        status: 'INACTIVE'
                    }
                },
                turboSale: {
                    status: 'inactive',
                    renewal: {
                        status: 'INACTIVE'
                    }
                },
                placement: {
                    status: 'inactive',
                    renewal: {
                        status: 'INACTIVE'
                    }
                }
            },
            newFlat: false,
            selected: false,
            excerptReportInfo: {
                status: {
                    reportIsAbsent: {}
                }
            },
            hasApartmentNumber: false,
            placement: {},
            isInCluster: false,
            isFromFeed: false,
            partnerId: '1035218734',
            partnerInternalId: '5409948112816228096',
            restrictions: []
    }, additionalParams)
}

export const getOffer = (additionalParams = {}) => {
    return merge({}, {
        id: '6422687922697113088',
        status: 'active',
        banReasons: [],
        isEditable: true,
        stats: {
            statsPeriod: 30,
            cardShow: {
                status: 'success',
                offerId: '6422687922697113088',
                details: [
                    {
                        timestamp: '2020-07-02T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-03T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-04T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-05T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-06T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-07T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-08T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-09T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-10T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-11T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-12T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-13T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-14T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-15T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-16T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-17T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-18T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-19T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-20T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-21T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-22T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-23T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-24T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-25T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-26T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-27T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-28T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-29T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-30T00:00:00.000+03:00',
                        value: 2
                    },
                    {
                        timestamp: '2020-07-31T00:00:00.000+03:00',
                        value: 0
                    }
                ],
                total: 74
            },
            offerShow: {
                status: 'success',
                offerId: '6422687922697113088',
                details: [
                    {
                        timestamp: '2020-07-02T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-03T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-04T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-05T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-06T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-07T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-08T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-09T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-10T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-11T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-12T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-13T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-14T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-15T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-16T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-17T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-18T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-19T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-20T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-21T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-22T00:00:00.000+03:00',
                        value: 3
                    },
                    {
                        timestamp: '2020-07-23T00:00:00.000+03:00',
                        value: 1
                    },
                    {
                        timestamp: '2020-07-24T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-25T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-26T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-27T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-28T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-29T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-30T00:00:00.000+03:00',
                        value: 8
                    },
                    {
                        timestamp: '2020-07-31T00:00:00.000+03:00',
                        value: 0
                    }
                ],
                total: 6971
            },
            phoneShow: {
                status: 'success',
                offerId: '6422687922697113088',
                details: [
                    {
                        timestamp: '2020-07-02T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-03T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-04T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-05T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-06T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-07T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-08T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-09T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-10T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-11T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-12T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-13T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-14T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-15T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-16T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-17T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-18T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-19T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-20T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-21T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-22T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-23T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-24T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-25T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-26T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-27T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-28T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-29T00:00:00.000+03:00',
                        value: 0
                    },
                    {
                        timestamp: '2020-07-30T00:00:00.000+03:00',
                        value: 5
                    },
                    {
                        timestamp: '2020-07-31T00:00:00.000+03:00',
                        value: 0
                    }
                ],
                total: 33
            },
            calls: {
                status: 'success',
                loading: false,
                total: 1,
                details: [
                    {
                        timestamp: '2020-07-21T21:00:00Z',
                        value: 1
                    }
                ]
            },
            aggregated: [
                {
                    timestamp: '2020-07-02T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-03T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-04T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-05T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-06T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-07T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-08T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-09T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-10T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-11T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-12T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-13T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-14T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-15T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-16T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-17T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-18T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-19T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-20T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-21T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-22T00:00:00.000+03:00',
                    value: {
                        offerShow: 3,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-23T00:00:00.000+03:00',
                    value: {
                        offerShow: 1,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-24T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-25T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-26T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-27T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-28T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-29T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                },
                {
                    timestamp: '2020-07-30T00:00:00.000+03:00',
                    value: {
                        offerShow: 8,
                        cardShow: 2,
                        phoneShow: 5
                    }
                },
                {
                    timestamp: '2020-07-31T00:00:00.000+03:00',
                    value: {
                        offerShow: 0,
                        cardShow: 0,
                        phoneShow: 0
                    }
                }
            ]
        },
        offerType: 'SELL',
        category: 'APARTMENT',
        areaValue: 120,
        areaUnit: 'SQ_M',
        currency: 'RUB',
        price: 8750000,
        rooms: '3',
        address: 'Санкт-Петербург, улица Профессора Попова, 5',
        location: {
            rgid: 417899,
            country: 'Россия',
            address: 'Санкт-Петербург, улица Профессора Попова, 5',
            latitude: 59.9719429,
            longitude: 30.32429314,
            streetAddress: 'улица Профессора Попова, 5',
            localityName: 'Санкт-Петербург',
            region: 'Санкт-Петербург',
            subLocalityName: 'Петроградский район',
            houseNumber: '5',
            street: 'улица Профессора Попова',
            subjectFederationId: 10174,
            metro: [
                {
                    name: 'Петроградская',
                    timeOnFoot: 14,
                    timeOnTransport: 14,
                    geoId: 20336
                }
            ],
            point: {
                longitude: 30.32429314,
                latitude: 59.9719429
            }
        },
        unifiedLocation: {
            rgid: 417899,
            region: 'Санкт-Петербург',
            localityName: 'Санкт-Петербург',
            subLocalityName: 'Петроградский район',
            shortAddress: 'улица Профессора Попова, 5',
            metroLineColor: [
                '16bdf0'
            ],
            subjectFederationId: 10174,
            street: 'улица Профессора Попова',
            houseNumber: '5',
            metro: [
                {
                    name: 'Петроградская',
                    timeOnFoot: 14,
                    timeOnTransport: 14,
                    geoId: 20336,
                    color: '#16bdf0',
                    transitionsColors: [
                        '#16bdf0'
                    ],
                    cityKey: '2',
                    lineName: 'Московско-Петроградская линия',
                    lineId: '2_2',
                    lines: [
                        {
                            id: '2_2',
                            cityId: 2,
                            name: 'Московско-Петроградская линия',
                            alias: 'Синяя',
                            isRing: false,
                            color: '#16bdf0',
                            stationsIds: [
                                20341,
                                102531,
                                20319,
                                20320,
                                20321,
                                20322,
                                20323,
                                20336,
                                20335,
                                20347,
                                20340,
                                20342,
                                20310,
                                20309,
                                20308,
                                20307,
                                20306,
                                20305
                            ]
                        }
                    ]
                },
                {
                    name: 'Выборгская',
                    timeOnFoot: 22,
                    timeOnTransport: 18,
                    geoId: 20330,
                    color: '#f03d2f',
                    transitionsColors: [
                        '#f03d2f'
                    ],
                    cityKey: '2',
                    lineName: 'Кировско-Выборгская линия',
                    lineId: '2_1',
                    lines: [
                        {
                            id: '2_1',
                            cityId: 2,
                            name: 'Кировско-Выборгская линия',
                            alias: 'Красная',
                            isRing: false,
                            color: '#f03d2f',
                            stationsIds: [
                                20325,
                                20326,
                                20327,
                                20328,
                                20329,
                                20318,
                                20330,
                                20331,
                                20355,
                                20354,
                                20353,
                                20343,
                                20341,
                                20344,
                                20304,
                                20303,
                                20302,
                                20301,
                                20300
                            ]
                        }
                    ]
                },
                {
                    name: 'Горьковская',
                    timeOnFoot: 28,
                    timeOnTransport: 19,
                    geoId: 20335,
                    color: '#16bdf0',
                    transitionsColors: [
                        '#16bdf0'
                    ],
                    cityKey: '2',
                    lineName: 'Московско-Петроградская линия',
                    lineId: '2_2',
                    lines: [
                        {
                            id: '2_2',
                            cityId: 2,
                            name: 'Московско-Петроградская линия',
                            alias: 'Синяя',
                            isRing: false,
                            color: '#16bdf0',
                            stationsIds: [
                                20341,
                                102531,
                                20319,
                                20320,
                                20321,
                                20322,
                                20323,
                                20336,
                                20335,
                                20347,
                                20340,
                                20342,
                                20310,
                                20309,
                                20308,
                                20307,
                                20306,
                                20305
                            ]
                        }
                    ]
                },
                {
                    name: 'Чкаловская',
                    timeOnFoot: 31,
                    timeOnTransport: 16,
                    geoId: 20333,
                    color: '#c063d1',
                    transitionsColors: [
                        '#c063d1'
                    ],
                    cityKey: '2',
                    lineName: 'Фрунзенско-Приморская линия',
                    lineId: '2_5',
                    lines: [
                        {
                            id: '2_5',
                            cityId: 2,
                            name: 'Фрунзенско-Приморская линия',
                            alias: 'Фиолетовая',
                            isRing: false,
                            color: '#c063d1',
                            stationsIds: [
                                21743,
                                20324,
                                20334,
                                20333,
                                20332,
                                114766,
                                20339,
                                100651,
                                110348,
                                100652,
                                114839,
                                114838,
                                218469,
                                218470,
                                218471
                            ]
                        }
                    ]
                }
            ]
        },
        photo: {
            orig: generateImageUrl({ width: 600, height: 400 }),
            main: generateImageUrl({ width: 600, height: 400 }),
            tag: ''
        },
        photos: [
            {
                orig: generateImageUrl({ width: 600, height: 400 }),
                main: generateImageUrl({ width: 600, height: 400 }),
                tag: ''
            }
        ],
        createTime: '2019-08-02T13:40:30.000Z',
        updateTime: '2020-04-14T14:27:52.000Z',
        endOfShow: '2020-08-12T14:27:52.000Z',
        showDuration: 120,
        services: {
            premium: {
                priceContext: {
                    isAvailable: true,
                    effective: 0,
                    base: 459,
                    reasons: [],
                    modifiers: {
                        promocode: 'PREMIUM'
                    }
                },
                description: {
                    duration: 7,
                    description: 'Премиум-объявления показываются\u2028 на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
                },
                isChangingStatus: false,
                isChangingNotCancelable: false,
                isWaitingForDeactivation: false,
                status: 'inactive',
                renewal: {
                    status: 'UNAVAILABLE'
                }
            },
            raising: {
                priceContext: {
                    isAvailable: true,
                    effective: 0,
                    base: 47,
                    reasons: [],
                    modifiers: {
                        promocode: 'RAISING'
                    }
                },
                description: {
                    duration: 1,
                    description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
                },
                isChangingStatus: false,
                isChangingNotCancelable: false,
                isWaitingForDeactivation: false,
                status: 'inactive',
                renewal: {
                    status: 'UNAVAILABLE'
                }
            },
            promotion: {
                priceContext: {
                    isAvailable: true,
                    effective: 139,
                    base: 139,
                    reasons: [],
                    modifiers: {}
                },
                description: {
                    duration: 7,
                    description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
                },
                isChangingStatus: false,
                isChangingNotCancelable: false,
                isWaitingForDeactivation: false,
                status: 'inactive',
                renewal: {
                    status: 'UNAVAILABLE'
                }
            },
            turboSale: {
                priceContext: {
                    isAvailable: true,
                    effective: 799,
                    base: 799,
                    reasons: [],
                    modifiers: {}
                },
                description: {
                    duration: 7,
                    description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» \u2028в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
                },
                isChangingStatus: false,
                isChangingNotCancelable: false,
                isWaitingForDeactivation: false,
                status: 'inactive',
                renewal: {
                    status: 'UNAVAILABLE'
                }
            },
            placement: {
                priceContext: {
                    isAvailable: false,
                    effective: null,
                    base: null,
                    reasons: [
                        'VST_FREE_PLACEMENT'
                    ],
                    modifiers: {}
                },
                description: {
                    duration: 30,
                    description: 'Платное размещение'
                },
                isChangingStatus: false,
                isChangingNotCancelable: false,
                isWaitingForDeactivation: false,
                status: 'inactive',
                renewal: {
                    status: 'UNAVAILABLE'
                }
            }
        },
        newFlat: false,
        openPlan: false,
        studio: false,
        selected: false,
        excerptReportInfo: {
            status: {
                missingDataForReport: {
                    missingData: [
                        'APARTMENT',
                        'CADASTRAL_NUMBER'
                    ]
                }
            }
        },
        hasApartmentNumber: false,
        placement: {},
        isInCluster: false,
        isFromFeed: false,
        partnerId: '1035218734',
        partnerInternalId: '6422687922697113088',
        restrictions: [],
        trustedOwnerInfo: {
            ownerTrustedStatus: 'NOT_LINKED_MOSRU'
        }
    }, additionalParams);
};

export const productsWithDiscount = {
    premium: {
        priceContext: {
            isAvailable: true,
            effective: 0,
            base: 329,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 30,
                    amount: 99
                },
                promocode: 'PREMIUM'
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Премиум-объявления показываются\u2028 на первых трёх позициях каждой страницы выдачи и отмечаются специальным значком. Плюс показываются на главной странице.'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    raising: {
        priceContext: {
            isAvailable: true,
            effective: 37,
            base: 37,
            reasons: [],
            modifiers: {}
        },
        description: {
            duration: 1,
            description: 'Ваше объявление 24 часа будет показываться выше других после блока «Премиум».'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'active',
        renewal: {
            status: 'ACTIVE'
        },
        isAppliedFromFeed: false,
        end: 1604585926135
    },
    promotion: {
        priceContext: {
            isAvailable: true,
            effective: 29,
            base: 99,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 70,
                    amount: 70
                }
            }
        },
        description: {
            duration: 7,
            description: 'Ваше объявление оказывается выше любых бесплатных на страницах поиска в течение 7‑ми дней.'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    turboSale: {
        priceContext: {
            isAvailable: true,
            effective: 349,
            base: 499,
            reasons: [],
            modifiers: {
                bonusDiscount: {
                    percent: 30,
                    amount: 150
                }
            }
        },
        description: {
            duration: 7,
            // eslint-disable-next-line max-len
            description: 'Включает в себя опции «Премиум», «Продвижение», ежедневное «Поднятие» \u2028в течение недели. Получите в 7 раз больше просмотров и в 3 раза больше звонков!'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    },
    placement: {
        priceContext: {
            isAvailable: false,
            effective: null,
            base: null,
            reasons: [
                'VST_CALCULATION_ERROR'
            ],
            modifiers: {}
        },
        description: {
            duration: 30,
            description: 'Платное размещение'
        },
        isChangingStatus: false,
        isChangingNotCancelable: false,
        isWaitingForDeactivation: false,
        status: 'inactive',
        renewal: {
            status: 'UNAVAILABLE'
        }
    }
};
