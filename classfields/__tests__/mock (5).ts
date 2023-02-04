import {
    AddressInfoStatus,
    BuildingHeatingType,
    BuildingType,
    EncumbranceType,
    IAddress,
    IBuildingData,
    IEGRNPaidReport,
    IEncumbrance,
    IHeatmap,
    IMetro,
    OfferBalconyType,
    OfferBathroomType,
    OfferWindowViewType,
    OwnerType,
    PaidReportStatus,
    PaymentStatus,
    VoteValue,
} from 'realty-core/view/react/common/types/egrnPaidReport';
import { IPark, IPond, TransportDistanceType } from 'realty-core/types/location';
import { IImage } from 'realty-core/types/common';

const address: IAddress = {
    addressInfoId: '123',
    userObjectInfo: {
        offerId: '12345',
        address: 'ул. Колесникова, 49, кв 38',
        flatNumber: '5',
        cadastralNumber: '1337',
    },
    evaluatedObjectInfo: {
        unifiedAddress: 'Россия, Санкт-Петербург, ул. Колесникова, 49, кв 38',
        floor: 'чердак',
        area: 55,
        cadastralNumber: '1337',
        subjectFederationId: 257,
        rrAddress: 'Россия, Санкт-Петербург, ул. Колесникова, 49, кв 38',
        unifiedRrAddress: 'Россия, Санкт-Петербург, ул. Колесникова, 49, кв 38',
    },
    status: AddressInfoStatus.DONE,
};

const buildingData: IBuildingData = {
    address: 'ул. Колесникова, 49, кв 38',
    availabilityIndexes: undefined,
    buildYear: 1999,
    buildingSeries: '19-к08',
    buildingType: BuildingType.BUILDING_TYPE_MONOLIT_BRICK,
    ceilingHeight: 269,
    expectDemolition: true,
    flatsCount: 88,
    hasElevator: true,
    hasRubbishChute: true,
    hasSecurity: true,
    heatingType: BuildingHeatingType.AUTONOMOUS,
    location: {
        latitude: 0, // карта не рендерится нормально в тестах (на скринах не появляется, для запуска нужно много разных обёрток)
        longitude: 0, // поэтому ставим тут нули и она не будет отрендерена
        defined: false,
    },
    metros: [
        {
            name: 'Ломоносовская',
            timeToMetro: 15,
            lineColors: ['ff0000'],
            rgbColor: '00ff00',
            metroGeoId: 123,
            latitude: 123,
            longitude: 123,
            metroTransport: TransportDistanceType.ON_CAR,
        },
        {
            name: 'Кржижановского',
            timeToMetro: 15,
            lineColors: ['ff0000'],
            rgbColor: '0000ff',
            metroGeoId: 123,
            latitude: 123,
            longitude: 123,
            metroTransport: TransportDistanceType.ON_FOOT,
        },
    ],
    numberOfFloors: 11,
    porchesCount: 3,
    reconstructionYear: 2008,
};

export const fullReport: IEGRNPaidReport = {
    address,
    addressInfoId: '123',
    annualTax: 0,
    buildingData,
    checks: {
        checkResults: [
            {
                historyCheck: {
                    lastDealLessThenFiveYearsAgo: {
                        period: 1,
                    },
                },
            },
            {
                historyCheck: {
                    lastDealLessThenSixMonthAgo: {
                        period: 1,
                    },
                },
            },
            {
                historyCheck: {
                    lastDealMoreThenFiveYearsAgo: {
                        period: 1,
                    },
                },
            },
            {
                historyCheck: {
                    moreThenOneChangeDuringLastThreeYears: {
                        number: 1,
                    },
                },
            },
            {
                historyCheck: {
                    unknown: {},
                },
            },
            {
                ownerCheck: {
                    naturalPerson: {
                        number: 3,
                    },
                },
            },
            {
                ownerCheck: {
                    notNaturalPerson: {},
                },
            },
            {
                ownerCheck: {
                    singleNaturalPerson: {},
                },
            },
            {
                ownerCheck: {
                    unknown: {},
                },
            },
            {
                encumbranceCheck: {
                    noEncumbrances: {},
                },
            },
            {
                encumbranceCheck: {
                    mortgage: {
                        encumbranceOwner: [],
                    },
                },
            },
            {
                encumbranceCheck: {
                    other: {
                        encumbranceType: [],
                    },
                },
            },
        ],
    },
    currentRights: {
        currentRights: [
            {
                owners: [
                    {
                        type: OwnerType.NATURAL_PERSON,
                        name: 'Иванов А.',
                    },
                ],
                registration: {
                    idRecord: '12345',
                    regNumber: '54321',
                    type: '',
                    name: '',
                    regDate: '2010-10-10',
                    shareText: '1/3',
                },
            },
            {
                owners: [
                    {
                        type: OwnerType.NATURAL_PERSON,
                        name: 'Иванова А.',
                    },
                ],
                registration: {
                    idRecord: '12345',
                    regNumber: '54321',
                    type: '',
                    name: '',
                    regDate: '2010-10-10',
                    shareText: '1/3',
                },
            },
        ],
        prevRights: [
            {
                owners: [
                    {
                        type: OwnerType.NATURAL_PERSON,
                        name: 'Иванов А.',
                    },
                ],
                registration: {
                    idRecord: '12345',
                    regNumber: '54321',
                    type: '',
                    name: '',
                    regDate: '2010-10-10',
                    shareText: '1/3',
                    endDate: '2015-10-10',
                },
            },
            {
                owners: [
                    {
                        type: OwnerType.NATURAL_PERSON,
                        name: 'Иванова А.',
                    },
                ],
                registration: {
                    idRecord: '12345',
                    regNumber: '54321',
                    type: '',
                    name: '',
                    regDate: '2010-10-10',
                    shareText: '1/3',
                    endDate: '2015-10-10',
                },
            },
        ],
    },
    email: 'someone@mail.ru',
    encumbrances: {
        encumbrances: [
            {
                type: EncumbranceType.ARREST,
            },
            {
                type: EncumbranceType.LEASE_SUBLEASE,
            },
            {
                type: EncumbranceType.MORTGAGE,
            },
            {
                type: EncumbranceType.OTHER_RESTRICTIONS,
            },
            {
                type: EncumbranceType.PROHIBITION,
            },
            {
                type: EncumbranceType.RENT,
            },
            {
                type: EncumbranceType.SEIZURE_DECISION,
            },
            {
                type: EncumbranceType.TRUST_MANAGEMENT,
            },
        ] as Array<IEncumbrance>,
    },
    excerptData: {
        address: 'ул. Колесникова, 49, кв 38',
        area: 61,
        floor: 'чердак',
        cadastralCost: 900000,
        date: '2020-10-10',
        cadastralNumber: '1337',
    },
    offerData: {
        address: 'ул. Колесникова, 49, кв 38',
        numberOfFloors: '1',
        bathroomType: OfferBathroomType.BATHROOM_TYPE_MATCHED,
        area: 59,
        floor: 'чердак',
        kitchenArea: 12,
        ceilingHeight: 233,
        balconyType: OfferBalconyType.BALCONY_TYPE_BALCONY,
        windowView: OfferWindowViewType.WINDOW_VIEW_STREET,
        roomsNumber: 2,
        expectDemolition: true,
        image: {} as IImage,
        pondList: [
            {
                name: 'Новоозёрский пруд',
                timeOnFoot: 420,
            },
            {
                name: 'Новопрудный пруд',
                timeOnFoot: 500,
            },
        ] as Array<IPond>,
        parkList: [
            {
                name: 'Девичий парк',
                timeOnFoot: 650,
            },
            {
                name: 'Женский парк',
                timeOnFoot: 600,
            },
        ] as Array<IPark>,
        metroList: [
            {
                name: 'Чувашская',
                timeToMetro: 20,
                metroTransport: TransportDistanceType.ON_FOOT,
            },
        ] as Array<IMetro>,
        heatmapList: [
            {
                title: 'Инфраструктура',
                description: 'Её крутость',
                level: 5,
                maxLevel: 9,
            },
            {
                title: 'Доступность',
                description: 'Так себе',
                level: 3,
                maxLevel: 9,
            },
            {
                title: 'Цена жилья',
                description: 'Дёшево',
                level: 8,
                maxLevel: 9,
            },
        ] as Array<IHeatmap>,
    },
    otherOffers: {
        firstOfferDay: '2015-10-10',
        daysInExposition: 69,
        totalOffers: 700,
        studios: 59,
        rooms1: 88,
        rooms2: 356,
        rooms3: 32,
        rooms4AndMore: 183,
    },
    paidReportId: '999999',
    paymentStatus: PaymentStatus.PAID,
    priceDynamics: {
        building: [
            { date: '2010-10-10', value: 30 },
            { date: '2011-01-10', value: 40 },
            { date: '2011-10-10', value: 50 },
            { date: '2012-10-10', value: 30 },
        ],
        fifteenMin: [
            { date: '2010-10-10', value: 22 },
            { date: '2011-01-10', value: 12 },
            { date: '2011-10-10', value: 93 },
            { date: '2012-10-10', value: 30 },
        ],
        district: [
            { date: '2010-10-10', value: 46 },
            { date: '2011-01-10', value: 78 },
            { date: '2011-10-10', value: 51 },
            { date: '2012-10-10', value: 30 },
            { date: '2013-01-10', value: 40 },
        ],
    },
    priceRange: {
        max: 10000000,
        min: 2000000,
        median: 500000,
        percentile25: 4000000,
        percentile75: 9000000,
        offersCount: 50,
    },
    reportDate: '2020-10-10',
    reportStatus: PaidReportStatus.DONE,
    tax: 0,
    uid: '123456',
    vote: {
        vote: VoteValue.POSITIVE,
        comment: 'sps za otchet',
        created: '2020-10-10',
    },
};
