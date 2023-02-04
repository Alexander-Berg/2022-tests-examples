import { IOfferCard, OfferType } from 'realty-core/types/offerCard';

export const offer = ({
    offerType: OfferType.SELL,
    excerptReport: {
        cadastralNumber: '73:02:0003454:****',
        createDate: '2019-12-16T11:29:19.456Z',
        flatReport: {
            buildingInfo: {
                area: 45,
                floor: 6,
            },
            costInfo: {
                cadastralCost: '6900000',
            },
            currentRights: [
                {
                    owners: [
                        {
                            type: 'NATURAL_PERSON',
                            name: 'И***** И.И.',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.433Z',
                        shareText: '1/2',
                    },
                },
                {
                    owners: [
                        {
                            type: 'NATURAL_PERSON',
                            name: 'И***** И.И.',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                        shareText: '1/2',
                    },
                },
            ],
            previousRights: [
                {
                    owners: [
                        {
                            type: 'NATURAL_PERSON',
                        },
                        {
                            type: 'NATURAL_PERSON',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                    },
                },
                {
                    owners: [
                        {
                            type: 'JURIDICAL_PERSON',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                    },
                },
                {
                    owners: [
                        {
                            type: 'GOVERNANCE',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                    },
                },
                {
                    owners: [
                        {
                            type: 'GOVERNANCE',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                    },
                },
                {
                    owners: [
                        {
                            type: 'GOVERNANCE',
                        },
                    ],
                    registration: {
                        idRecord: '732735771877',
                        regNumber: '77-77/007-77/007/003/2015-1474/3',
                        type: 'OWNERSHIP',
                        name: 'Собственность, № 77-77/007-77/007/003/2015-1474/3 от 22.06.2015',
                        regDate: '2019-12-16T11:29:19.455Z',
                    },
                },
            ],
            encumbrances: [
                {
                    type: 'MORTGAGE',
                    regDate: '2019-12-16T11:29:19.456Z',
                    regNumber: '1234567890',
                    duration: 'Срок',
                    startDate: '2019-12-16T11:29:19.456Z',
                },
                {
                    type: 'OTHER_RESTRICTIONS',
                    regDate: '2019-12-16T11:29:19.456Z',
                    regNumber: '1234567890',
                    duration: 'Срок',
                    startDate: '2019-12-16T11:29:19.456Z',
                },
            ],
        },
    },
} as unknown) as IOfferCard;
