import { getShortTitle } from '../';

describe('Get short title', () => {
    describe('for APARTMENT category', () => {
        const baseApartmentOffer = {
            offerCategory: 'APARTMENT'
        };

        it('return only title', () => {
            expect(getShortTitle(baseApartmentOffer)).toEqual('квартира');
        });

        it('return title with rooms', () => {
            expect(getShortTitle({
                ...baseApartmentOffer,
                roomsTotal: 3
            })).toEqual('3-комнатная квартира');
        });

        it('return title with open plan', () => {
            expect(getShortTitle({
                ...baseApartmentOffer,
                openPlan: true
            })).toEqual('свободная планировка');
        });

        it('return title with studio', () => {
            expect(getShortTitle({
                ...baseApartmentOffer,
                house: {
                    studio: true
                }
            })).toEqual('студия');
        });

        it('return title with 3 rooms and area', () => {
            expect(getShortTitle({
                ...baseApartmentOffer,
                area: {
                    value: 65,
                    unit: 'SQUARE_METER'
                },
                roomsTotal: 3
            })).toEqual('65 м², 3-комнатная квартира');
        });
    });

    describe('for ROOMS category', () => {
        const baseRoomsOffer = {
            offerCategory: 'ROOMS'
        };

        it('return only title category', () => {
            expect(getShortTitle(baseRoomsOffer)).toEqual('комната');
        });

        it('return title with 1 offered room in 3 rooms apartment', () => {
            expect(getShortTitle({
                ...baseRoomsOffer,
                roomsTotal: 3,
                roomsOffered: 1
            })).toEqual('комната в 3-комнатной квартире');
        });

        it('return title with 3 offered rooms in 5 rooms apartment', () => {
            expect(getShortTitle({
                ...baseRoomsOffer,
                roomsTotal: 5,
                roomsOffered: 3
            })).toEqual('3 комнаты в 5-комнатной квартире');
        });

        it('return title with 3 rooms count', () => {
            expect(getShortTitle({
                ...baseRoomsOffer,
                roomsOffered: 3
            })).toEqual('3 комнаты');
        });

        it('return title with area', () => {
            expect(getShortTitle({
                ...baseRoomsOffer,
                roomsOffered: 1,
                roomsTotal: 2,
                area: {
                    value: 15,
                    unit: 'SQUARE_METER'
                }
            })).toEqual('15 м², комната в 2-комнатной квартире');
        });
    });

    describe('for HOUSE category', () => {
        const baseHouseOffer = {
            offerCategory: 'HOUSE'
        };

        it('должен возвращать заголовок категории дом', () => {
            expect(getShortTitle(baseHouseOffer)).toEqual('дом');
        });

        it('должен возвращать заголовок для дома', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                house: {
                    houseType: 'HOUSE'
                }
            })).toEqual('дом');
        });

        it('должен возвращать заголовок для дома с участком', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                lot: {
                    lotArea: 10
                }
            })).toEqual('дом с уч.');
        });

        it('должен возвращать заголовок для части дома', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                house: {
                    houseType: 'PARTHOUSE'
                }
            })).toEqual('часть дома');
        });

        it('должен возвращать заголовок для таунхауса', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                house: {
                    houseType: 'TOWNHOUSE'
                }
            })).toEqual('таунхаус');
        });

        it('должен возвращать заголовок для дуплекса', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                house: {
                    houseType: 'DUPLEX'
                }
            })).toEqual('дуплекс');
        });

        it('должен возвращать заголовок для таунхауса с площадью и участком', () => {
            expect(getShortTitle({
                ...baseHouseOffer,
                house: {
                    houseType: 'TOWNHOUSE'
                },
                lot: {
                    lotArea: 10
                },
                area: {
                    value: 50,
                    unit: 'SQUARE_METER'
                }
            })).toEqual('50 м², таунхаус с уч.');
        });
    });

    describe('for GARAGE category', () => {
        const baseGarageOffer = {
            offerCategory: 'GARAGE'
        };

        it('return only title category', () => {
            expect(getShortTitle(baseGarageOffer)).toEqual('гараж');
        });

        it('return title if category same', () => {
            expect(getShortTitle({
                ...baseGarageOffer,
                garage: {
                    garageType: 'GARAGE'
                }
            })).toEqual('гараж');
        });

        it('return title if type BOX', () => {
            expect(getShortTitle({
                ...baseGarageOffer,
                garage: {
                    garageType: 'BOX'
                }
            })).toEqual('бокс');
        });

        it('return title if type PARKING_PLACE', () => {
            expect(getShortTitle({
                ...baseGarageOffer,
                garage: {
                    garageType: 'PARKING_PLACE'
                }
            })).toEqual('машиноместо');
        });

        it('return title if type BOX with area', () => {
            expect(getShortTitle({
                ...baseGarageOffer,
                garage: {
                    garageType: 'BOX'
                },
                area: {
                    value: 5,
                    unit: 'SQUARE_METER'
                }
            })).toEqual('5 м², бокс');
        });
    });

    describe('for LOT category', () => {
        const baseLotOffer = {
            offerCategory: 'LOT'
        };

        it('return only title category', () => {
            expect(getShortTitle(baseLotOffer)).toEqual('участок');
        });

        it('return lot title with meters area', () => {
            expect(getShortTitle({
                ...baseLotOffer,
                area: {
                    value: 10,
                    unit: 'SQUARE_METER'
                }
            })).toEqual('10 м², участок');
        });

        it('return lot title with are area', () => {
            expect(getShortTitle({
                ...baseLotOffer,
                area: {
                    value: 2,
                    unit: 'ARE'
                }
            })).toEqual('2 сотки, участок');
        });

        it('return lot title with hectare area', () => {
            expect(getShortTitle({
                ...baseLotOffer,
                area: {
                    value: 5,
                    unit: 'HECTARE'
                }
            })).toEqual('5 гектаров, участок');
        });
    });

    describe('for COMMERCIAL category', () => {
        const baseCommercialOffer = {
            offerCategory: 'COMMERCIAL'
        };

        it('return only title category', () => {
            expect(getShortTitle(baseCommercialOffer)).toEqual('коммерческая недвижимость');
        });

        it('return title if type AUTO_REPAIR', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'AUTO_REPAIR' ]
                }
            })).toEqual('автосервис');
        });

        it('return title if type FREE_PURPOSE', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'FREE_PURPOSE' ]
                }
            })).toEqual('помещение свободного назначения');
        });

        it('return title if type LAND', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'LAND' ]
                }
            })).toEqual('земля коммерческого назначения');
        });

        it('return title if type MANUFACTURING', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'MANUFACTURING' ]
                }
            })).toEqual('производственное помещение');
        });

        it('return title if type OFFICE', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'OFFICE' ]
                }
            })).toEqual('офис');
        });

        it('return title if type WAREHOUSE', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'WAREHOUSE' ]
                }
            })).toEqual('склад');
        });

        it('return title if type OFFICE with area', () => {
            expect(getShortTitle({
                ...baseCommercialOffer,
                commercial: {
                    commercialTypes: [ 'OFFICE' ]
                },
                area: {
                    value: 40,
                    unit: 'SQUARE_METER'
                }
            })).toEqual('40 м², офис');
        });
    });
});
