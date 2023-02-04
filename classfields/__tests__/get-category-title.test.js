import i18n from 'realty-core/view/react/libs/i18n';
import { getCategoryTitle, getProfCategoryTitle } from '../';

describe('getCategoryTitle', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    describe('if category is "ROOMS"', () => {
        const defaultRoomsOffer = {
            category: 'ROOMS',
            rooms: 7,
            roomsOffered: 1
        };

        it('shows title for 1 offered room', () => {
            const result = getCategoryTitle(defaultRoomsOffer);

            expect(result).toEqual('комната в 7-комнатной квартире');
        });

        it('shows title if room counts are not specified', () => {
            const result = getCategoryTitle({ category: 'ROOMS' });

            expect(result).toEqual('комната');
        });

        it('shows title if total room count is not known', () => {
            const result = getCategoryTitle({ category: 'ROOMS', roomsOffered: 3 });

            expect(result).toEqual('3 комнаты');
        });

        it('shows title for 2 offered room', () => {
            const offer = { ...defaultRoomsOffer, roomsOffered: 2 };
            const result = getCategoryTitle(offer);

            expect(result).toEqual('2 комнаты в 7-комнатной квартире');
        });

        it('shows title for 5 offered room', () => {
            const offer = { ...defaultRoomsOffer, roomsOffered: 5 };
            const result = getCategoryTitle(offer);

            expect(result).toEqual('5 комнат в 7-комнатной квартире');
        });

        it('shows short title for rooms', () => {
            const offer = { ...defaultRoomsOffer, roomsOffered: 5 };
            const result = getCategoryTitle(offer, { short: true });

            expect(result).toEqual('5 комнат в 7-к. кв.');
        });

        it('shows area', () => {
            const result = getCategoryTitle({
                ...defaultRoomsOffer,
                areaUnit: 'SQ_M',
                livingSpace: 10
            }, { withArea: true });

            expect(result).toEqual('10 м², комната в 7-комнатной квартире');
        });
    });

    describe('if category is "APARTMENT"', () => {
        const defaultApartmentOffer = {
            category: 'APARTMENT'
        };

        it('shows title for category', () => {
            expect(getCategoryTitle(defaultApartmentOffer))
                .toEqual('квартира');
        });

        it('shows title for rooms', () => {
            expect(getCategoryTitle({
                ...defaultApartmentOffer,
                rooms: 2
            })).toEqual('2-комнатная квартира');
        });

        it('shows short title for rooms', () => {
            expect(getCategoryTitle({
                ...defaultApartmentOffer,
                rooms: 2
            }, { short: true })).toEqual('2-комнатная кв.');
        });

        it('shows title for open plan', () => {
            expect(getCategoryTitle({
                ...defaultApartmentOffer,
                openPlan: true
            })).toEqual('свободная планировка');
        });

        it('shows title for studio', () => {
            expect(getCategoryTitle({
                ...defaultApartmentOffer,
                studio: true
            })).toEqual('студия');
        });

        it('shows area', () => {
            const result = getCategoryTitle({
                ...defaultApartmentOffer,
                areaUnit: 'SQ_M',
                areaValue: 20
            }, { withArea: true });

            expect(result).toEqual('20 м², квартира');
        });
    });

    describe('if category is "HOUSE"', () => {
        const defaultHouseOffer = {
            category: 'HOUSE'
        };

        it('shows title for house', () => {
            expect(getCategoryTitle(defaultHouseOffer)).toEqual('дом');

            expect(getCategoryTitle({
                ...defaultHouseOffer,
                houseType: 'HOUSE'
            })).toEqual(getCategoryTitle(defaultHouseOffer));
        });

        it('shows title for house with lot', () => {
            expect(getCategoryTitle({ ...defaultHouseOffer, lotAreaValue: 1 })).toEqual('дом с уч.');
        });

        it('shows title for parthouse', () => {
            expect(getCategoryTitle({
                ...defaultHouseOffer,
                houseType: 'PARTHOUSE'
            })).toEqual('часть дома');

            expect(getCategoryTitle({
                ...defaultHouseOffer,
                houseType: 'PARTHOUSE',
                lotAreaValue: 1
            })).toEqual('часть дома с уч.');
        });

        it('shows title for townhouse', () => {
            expect(getCategoryTitle({
                ...defaultHouseOffer,
                houseType: 'TOWNHOUSE'
            })).toEqual('таунхаус');
        });

        it('shows title for duplex', () => {
            expect(getCategoryTitle({
                ...defaultHouseOffer,
                houseType: 'DUPLEX'
            })).toEqual('дуплекс');
        });

        it('shows area', () => {
            const result = getCategoryTitle({
                ...defaultHouseOffer,
                areaUnit: 'SQ_M',
                areaValue: 30
            }, { withArea: true });

            expect(result).toEqual('30 м², дом');
        });
    });

    describe('if category is "COMMERCIAL"', () => {
        const defaultCommercialOffer = {
            category: 'COMMERCIAL'
        };

        it('shows title for category', () => {
            expect(getCategoryTitle(defaultCommercialOffer))
                .toEqual('коммерческая недвижимость');

            expect(getCategoryTitle(defaultCommercialOffer, { short: true }))
                .toEqual('коммерческая');
        });

        it('shows title for AUTO_REPAIR', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'AUTO_REPAIR'
            })).toEqual('автосервис');
        });

        it('shows title for FREE_PURPOSE', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'FREE_PURPOSE'
            })).toEqual('помещение свободного назначения');

            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'FREE_PURPOSE'
            }, { short: true })).toEqual('свободного назначения');
        });

        it('shows title for LAND', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'LAND'
            })).toEqual('земля коммерческого назначения');

            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'LAND'
            }, { short: true })).toEqual('земля комм. назначения');
        });

        it('shows title for MANUFACTURING', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'MANUFACTURING'
            })).toEqual('производственное помещение');

            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'MANUFACTURING'
            }, { short: true })).toEqual('произв. помещение');
        });

        it('shows title for OFFICE', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'OFFICE'
            })).toEqual('офис');

            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'OFFICE'
            }, { short: true })).toEqual('офис');
        });

        it('shows title for WAREHOUSE', () => {
            expect(getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'WAREHOUSE'
            })).toEqual('склад');
        });

        it('shows area', () => {
            const result = getCategoryTitle({
                ...defaultCommercialOffer,
                areaUnit: 'SQ_M',
                areaValue: 50
            }, { withArea: true });

            expect(result).toEqual('50 м², коммерческая недвижимость');
        });

        it('shows area if commercialType is LAND', () => {
            const result = getCategoryTitle({
                ...defaultCommercialOffer,
                commercialType: 'LAND',
                lotAreaUnit: 'SOTKA',
                lotAreaValue: 50
            }, { withArea: true });

            expect(result).toEqual('50 соток, земля коммерческого назначения');
        });
    });

    describe('if category is "GARAGE"', () => {
        const defaultGarageOffer = {
            category: 'GARAGE'
        };

        it('shows title for garage', () => {
            expect(getCategoryTitle(defaultGarageOffer)).toEqual('гараж');

            expect(getCategoryTitle({
                ...defaultGarageOffer,
                garageType: 'GARAGE'
            })).toEqual(getCategoryTitle(defaultGarageOffer));
        });

        it('shows title for box', () => {
            expect(getCategoryTitle({
                ...defaultGarageOffer,
                garageType: 'BOX'
            })).toEqual('бокс');
        });

        it('shows title for parking place', () => {
            expect(getCategoryTitle({
                ...defaultGarageOffer,
                garageType: 'PARKING_PLACE'
            })).toEqual('машиноместо');
        });

        it('shows area', () => {
            const result = getCategoryTitle({
                ...defaultGarageOffer,
                areaUnit: 'SQ_M',
                areaValue: 5
            }, { withArea: true });

            expect(result).toEqual('5 м², гараж');
        });
    });

    describe('if category is "LOT"', () => {
        const defaultLotOffer = {
            category: 'LOT'
        };

        it('shows title for lot', () => {
            expect(getCategoryTitle(defaultLotOffer)).toEqual('участок');
        });

        it('shows area (SOTKA)', () => {
            const result = getCategoryTitle({
                ...defaultLotOffer,
                lotAreaUnit: 'SOTKA',
                lotAreaValue: 5
            }, { withArea: true });

            expect(result).toEqual('5 соток, участок');
        });

        it('shows area (HECTARE)', () => {
            const result = getCategoryTitle({
                ...defaultLotOffer,
                lotAreaUnit: 'HECTARE',
                lotAreaValue: 3
            }, { withArea: true });

            expect(result).toEqual('3 гектара, участок');
        });
    });
});

describe('getProfCategoryTitle', () => {
    describe('if category is "COMMERCIAL"', () => {
        it('shows title for category with two commercial types', () => {
            const commercialOffer = {
                offerCategory: 'COMMERCIAL',
                commercial: {
                    commercialTypes: [ 'OFFICE', 'LEGAL_ADDRESS' ]
                },
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(getProfCategoryTitle(commercialOffer)).toEqual('коммерческая');
        });

        it('shows title for a certain commercial type', () => {
            const commercialOffer = {
                offerCategory: 'COMMERCIAL',
                commercial: {
                    commercialTypes: [ 'OFFICE' ]
                },
                commercialBuildingType: 'BUSINESS_CENTER'
            };

            expect(getProfCategoryTitle(commercialOffer)).toEqual('офис');
        });
    });
});
