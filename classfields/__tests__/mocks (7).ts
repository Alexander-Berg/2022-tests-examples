import { ISeoSiteSerpLinksOwnProps } from '../index';

interface IMockData {
    allFlats: ISeoSiteSerpLinksOwnProps;
    onlyNewRooms: ISeoSiteSerpLinksOwnProps;
    onlyResaleRooms: ISeoSiteSerpLinksOwnProps;
    smallAmountOfOffers: ISeoSiteSerpLinksOwnProps;
}

export const mocks: IMockData = {
    allFlats: {
        className: 'OffersSerp__siteSerpLinks',
        isMobile: false,
        site: {
            id: 1686722,
            name: 'Шереметьевский',
            locativeFullName: 'в жилом комплексе «Шереметьевский»',
            location: {
                populatedRgid: 741964,
            },
            rooms: [
                {
                    rooms: 'PLUS_4',
                    number: 4,
                },
                {
                    rooms: 'THREE',
                    number: 18,
                },
                {
                    rooms: 'STUDIO',
                    number: 11,
                },
                {
                    rooms: 'ONE',
                    number: 23,
                },
                {
                    rooms: 'TWO',
                    number: 55,
                },
            ],
            resaleRooms: [
                {
                    rooms: 'ONE',
                    number: 4,
                },
                {
                    rooms: 'THREE',
                    number: 18,
                },
                {
                    rooms: 'PLUS_4',
                    number: 23,
                },
                {
                    rooms: 'TWO',
                    number: 55,
                },
            ],
        },
    },
    onlyNewRooms: {
        className: 'OffersSerp__siteSerpLinks',
        isMobile: false,
        site: {
            id: 1686722,
            name: 'Шереметьевский',
            locativeFullName: 'в жилом комплексе «Шереметьевский»',
            location: {
                populatedRgid: 741964,
            },
            rooms: [
                {
                    rooms: 'PLUS_4',
                    number: 2,
                },
                {
                    rooms: 'THREE',
                    number: 14,
                },
                {
                    rooms: 'STUDIO',
                    number: 45,
                },
                {
                    rooms: 'ONE',
                    number: 21,
                },
                {
                    rooms: 'TWO',
                    number: 34,
                },
            ],
            resaleRooms: [],
        },
    },
    onlyResaleRooms: {
        className: 'OffersSerp__siteSerpLinks',
        isMobile: false,
        site: {
            id: 1686722,
            name: 'Шереметьевский',
            locativeFullName: 'в жилом комплексе «Шереметьевский»',
            location: {
                populatedRgid: 741964,
            },
            resaleRooms: [
                {
                    rooms: 'PLUS_4',
                    number: 7,
                },
                {
                    rooms: 'THREE',
                    number: 45,
                },
                {
                    rooms: 'STUDIO',
                    number: 13,
                },
                {
                    rooms: 'ONE',
                    number: 5,
                },
                {
                    rooms: 'TWO',
                    number: 42,
                },
            ],
            rooms: [],
        },
    },
    smallAmountOfOffers: {
        className: 'OffersSerp__siteSerpLinks',
        isMobile: false,
        site: {
            id: 1686722,
            name: 'Шереметьевский',
            locativeFullName: 'в жилом комплексе «Шереметьевский»',
            location: {
                populatedRgid: 741964,
            },
            rooms: [
                {
                    rooms: 'PLUS_4',
                    number: 3,
                },
                {
                    rooms: 'THREE',
                    number: 2,
                },
                {
                    rooms: 'STUDIO',
                    number: 2,
                },
                {
                    rooms: 'ONE',
                    number: 2,
                },
                {
                    rooms: 'TWO',
                    number: 3,
                },
            ],
            resaleRooms: [
                {
                    rooms: 'PLUS_4',
                    number: 3,
                },
                {
                    rooms: 'THREE',
                    number: 2,
                },
                {
                    rooms: 'STUDIO',
                    number: 3,
                },
                {
                    rooms: 'ONE',
                    number: 2,
                },
                {
                    rooms: 'TWO',
                    number: 2,
                },
            ],
        },
    },
};
