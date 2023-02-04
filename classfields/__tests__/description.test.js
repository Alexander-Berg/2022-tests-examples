import i18n from 'realty-core/view/react/libs/i18n';
import { getDescriptionByOffer, getGeneralDescription } from '../get-description';

const sellApartment = {
    offerCategory: 'APARTMENT',
    offerType: 'SELL'
};

const rentApartment = {
    offerCategory: 'APARTMENT',
    offerType: 'RENT'
};

const sellRooms = {
    offerCategory: 'ROOMS',
    offerType: 'SELL'
};

const rentRooms = {
    offerCategory: 'ROOMS',
    offerType: 'RENT'
};

const sellHouse = {
    offerCategory: 'HOUSE',
    offerType: 'SELL'
};

const rentHouse = {
    offerCategory: 'HOUSE',
    offerType: 'RENT'
};

const sellCommercial = {
    offerCategory: 'COMMERCIAL',
    offerType: 'SELL'
};

const rentCommercial = {
    offerCategory: 'COMMERCIAL',
    offerType: 'RENT'
};

describe('getDescriptionByOffer', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    describe('for apartment', () => {
        it('returns floors info', () => {
            const offerMock = {
                ...sellApartment,
                floorsTotal: 10,
                floorsOffered: [ 2 ]
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Этаж 2 из 10');
        });

        it('returns floors info if there is floorsTotal only', () => {
            const offerMock = {
                ...sellApartment,
                floorsTotal: 2
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('2 этажа');
        });

        it('returns building type info', () => {
            const offerMock = {
                ...sellApartment,
                building: {
                    buildingType: 'BRICK'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Кирпичный дом');
        });

        it('returns mortgage info', () => {
            const offerMock = {
                ...sellApartment,
                transactionConditionsMap: {
                    MORTGAGE: true
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Ипотека');
        });

        it('returns deal status info', () => {
            const offerMock = {
                ...sellApartment,
                dealStatus: 'SALE'
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Свободная продажа');
        });

        it('returns newbuilding info', () => {
            const mock = {
                ...sellApartment,
                flatType: 'NEW_FLAT'
            };

            expect(getDescriptionByOffer(mock)).toEqual([ 'Новостройка' ]);
        });

        it('doesnt return newbuilding info for secondary new', () => {
            const mock = {
                ...sellApartment,
                flatType: 'NEW_SECONDARY'
            };

            expect(getDescriptionByOffer(mock)).toEqual([]);
        });

        it('returns properly sorted description', () => {
            const offerMock = {
                ...sellApartment,
                transactionConditionsMap: {
                    MORTGAGE: true
                },
                building: {
                    buildingType: 'BRICK'
                },
                floorsTotal: 10,
                floorsOffered: [ 2 ]
            };

            const description = getDescriptionByOffer(offerMock);
            const expectedResult = [
                'Этаж 2 из 10',
                'Кирпичный дом',
                'Ипотека'
            ];

            expect(description).toEqual(expectedResult);
        });

        it('returns properly prioritized description', () => {
            const offerMock = {
                ...sellApartment,
                flatType: 'NEW_FLAT',
                transactionConditionsMap: {
                    MORTGAGE: true
                },
                building: {
                    buildingType: 'BRICK'
                },
                floorsTotal: 10,
                floorsOffered: [ 2 ]
            };

            const description = getDescriptionByOffer(offerMock);
            const expectedResult = [
                'Новостройка',
                'Этаж 2 из 10',
                'Кирпичный дом',
                'Ипотека'
            ];

            expect(description).toEqual(expectedResult);
        });

        describe('if offerType is "RENT"', () => {
            it('returns furniture info', () => {
                const offerMock = {
                    ...rentApartment,
                    apartment: {
                        improvements: {
                            NO_FURNITURE: false
                        }
                    }
                };

                const description = getDescriptionByOffer(offerMock);

                expect(description[0]).toBe('Мебель');
            });

            it('returns aircondition info', () => {
                const offerMock = {
                    ...rentApartment,
                    apartment: {
                        improvements: {
                            AIRCONDITION: true
                        }
                    }
                };

                const description = getDescriptionByOffer(offerMock);

                expect(description[0]).toBe('Кондиционер');
            });

            it('returns withPets', () => {
                const offerMock = {
                    ...rentApartment,
                    rentConditionsMap: {
                        PETS: true
                    }
                };

                const description = getDescriptionByOffer(offerMock);

                expect(description[0]).toBe('Можно с животными');
            });

            it('returns withChildren', () => {
                const offerMock = {
                    ...rentApartment,
                    rentConditionsMap: {
                        CHILDREN: true
                    }
                };

                const description = getDescriptionByOffer(offerMock);

                expect(description[0]).toBe('Можно с детьми');
            });
        });
    });

    describe('for rooms', () => {
        it('returns same as for apartments', () => {
            const commonProperties = {
                transactionConditionsMap: {
                    MORTGAGE: true
                },
                building: {
                    buildingType: 'BRICK'
                },
                floorsTotal: 10,
                floorsOffered: [ 2 ]
            };

            expect(getDescriptionByOffer({
                ...sellRooms,
                ...commonProperties
            })).toEqual(getDescriptionByOffer({
                ...sellApartment,
                ...commonProperties
            }));

            expect(getDescriptionByOffer({
                ...rentRooms,
                ...commonProperties
            })).toEqual(getDescriptionByOffer({
                ...rentApartment,
                ...commonProperties
            }));
        });
    });

    describe('for house', () => {
        it('returns floors info', () => {
            const offerMock = {
                ...sellHouse,
                floorsTotal: 10
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('10 этажей');
        });

        it('returns porperly sorted supply info', () => {
            const offerMock = {
                ...sellHouse,
                supplyMap: {
                    ELECTRICITY: true,
                    GAS: true,
                    WATER: true,
                    HEATING: true,
                    SEWERAGE: true
                }
            };

            const description = getDescriptionByOffer(offerMock);
            const expectedResult = [
                'Электричество',
                'Газ',
                'Отопление',
                'Водопровод'
            ];

            expect(description).toEqual(expectedResult);
        });

        it('returns porperly sorted supply info wihout one element', () => {
            const offerMock = {
                ...sellHouse,
                supplyMap: {
                    GAS: true,
                    WATER: true,
                    HEATING: true,
                    SEWERAGE: true
                }
            };

            const description = getDescriptionByOffer(offerMock);
            const expectedResult = [
                'Газ',
                'Отопление',
                'Водопровод',
                'Канализация'
            ];

            expect(description).toEqual(expectedResult);
        });

        it('returns pmg info (sell)', () => {
            const offerMock = {
                ...sellHouse,
                house: {
                    pmg: true
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Возможность ПМЖ');
        });

        it('returns pmg info (rent)', () => {
            const offerMock = {
                ...rentHouse,
                house: {
                    pmg: true
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Возможность ПМЖ');
        });
    });

    describe('for lot', () => {
        it('returns lot type info', () => {
            const offerMock = {
                offerCategory: 'LOT',
                lot: {
                    lotType: 'GARDEN'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Садоводство');
        });
    });

    describe('for garage', () => {
        it('does not return floors info in the description', () => {
            const offerMock = {
                ...sellApartment,
                offerCategory: 'GARAGE',
                floorsTotal: 2
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe(undefined);
        });

        it('returns mortgage info', () => {
            const offerMock = {
                ...sellApartment,
                transactionConditionsMap: {
                    MORTGAGE: true
                },
                offerCategory: 'GARAGE',
                floorsTotal: 2
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Ипотека');
        });
    });

    describe('for commercial', () => {
        it('returns lot type info', () => {
            const offerMock = {
                ...sellCommercial,
                house: {
                    entranceType: 'SEPARATE'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Отдельный');
        });

        it('должен возвращать, что пропускная система есть', () => {
            const offerMock = {
                ...sellCommercial,
                building: {
                    improvements: {
                        ACCESS_CONTROL_SYSTEM: true
                    }
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Пропускная система - есть');
        });

        it('должен возвращать, что пропускной системы нет', () => {
            const offerMock = {
                ...sellCommercial,
                building: {
                    improvements: {
                        ACCESS_CONTROL_SYSTEM: false
                    }
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Пропускная система - нет');
        });

        it('returns parking info', () => {
            const offerMock = {
                ...sellCommercial,
                building: {
                    parkingType: 'SECURE'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('Охраняемая парковка');
        });

        it('returns BC name info', () => {
            const offerMock = {
                ...sellCommercial,
                commercial: {
                    commercialBuildingType: 'BUSINESS_CENTER'
                },
                building: {
                    siteName: 'БЦ Тестовый'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('БЦ Тестовый');
        });

        it('returns BC class info', () => {
            const offerMock = {
                ...sellCommercial,
                commercial: {
                    commercialBuildingType: 'BUSINESS_CENTER'
                },
                building: {
                    officeClass: 'B_PLUS'
                }
            };

            const description = getDescriptionByOffer(offerMock);

            expect(description[0]).toBe('БЦ класса B+');
        });

        describe('if offerType is "RENT"', () => {
            it('returns deal status info', () => {
                const offerMock = {
                    ...rentCommercial,
                    dealStatus: 'DIRECT_RENT'
                };

                const description = getDescriptionByOffer(offerMock);

                expect(description[0]).toBe('Прямая аренда');
            });
        });
    });
});

describe('getGeneralDescription', () => {
    const offer = {
        building: {
            officeClass: 'A_PLUS',
            builtYear: 1999
        },
        house: {
            bathroomUnit: 'MATCHED',
            windowView: 'YARD'
        },
        apartment: {
            renovation: 'EURO'
        },
        area: {
            value: 10,
            unit: 'ARE'
        },
        livingSpace: {
            value: 15,
            unit: 'HECTARE'
        },
        kitchenSpace: {
            value: 20,
            unit: 'SQUARE_METER'
        },
        floorsOffered: [ 100, 100 ],
        roomsTotal: 2,
        floorCovering: 'LAMINATE'
    };

    const offerData = {
        roomSpace: [
            {
                value: 10,
                unit: 'ARE'
            },
            {
                value: 15,
                unit: 'HECTARE'
            },
            {
                value: 20,
                unit: 'SQUARE_METER'
            }
        ]
    };

    beforeEach(() => {
        i18n.setLang('ru');
    });

    it('empty', () => {
        expect(getGeneralDescription({})).toEqual([]);
    });

    it('without offerData', () => {
        const expectedResult = [
            { text: 'A+\u00a0класс' },
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '1999\u00a0год постройки' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];

        expect(getGeneralDescription(offer)).toEqual(expectedResult);
    });

    it('with offerData', () => {
        const expectedResult = [
            { text: 'A+\u00a0класс' },
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '10\u00a0сот,\u00a0 / 15\u00a0га\u00a0 / 20\u00a0м²\u00a0комнаты' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '1999\u00a0год постройки' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];

        expect(getGeneralDescription(offer, offerData)).toEqual(expectedResult);
    });

    it('returns year info with a building status', () => {
        const offerWithBuildingState = {
            ...offer,
            building: {
                builtYear: 2020,
                buildingState: 'UNFINISHED'
            }
        };

        const expectedResult = [
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '2020\u00a0год постройки, строится' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];
        const description = getGeneralDescription(offerWithBuildingState);

        expect(description).toEqual(expectedResult);
    });

    it('returns year info with a building status (for built houses)', () => {
        const offerWithBuildingState = {
            ...offer,
            building: {
                builtYear: 2020,
                buildingState: 'HAND_OVER'
            }
        };

        const expectedResult = [
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '2020\u00a0год постройки, сдан' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];
        const description = getGeneralDescription(offerWithBuildingState);

        expect(description).toEqual(expectedResult);
    });

    it('returns year info with the BUILT status', () => {
        const offerWithBuildingState = {
            ...offer,
            building: {
                builtYear: 2020,
                buildingState: 'BUILT'
            }
        };

        const expectedResult = [
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '2020\u00a0год постройки, построен' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];
        const description = getGeneralDescription(offerWithBuildingState);

        expect(description).toEqual(expectedResult);
    });

    it('returns year info with the CONSTRUCTION_SUSPENDED status', () => {
        const offerWithBuildingState = {
            ...offer,
            building: {
                builtYear: 2020,
                buildingState: 'CONSTRUCTION_SUSPENDED'
            }
        };

        const expectedResult = [
            { text: '10\u00a0сот,\u00a0общая' },
            { text: '15\u00a0га\u00a0жилая' },
            { text: '20\u00a0м²\u00a0кухня' },
            { text: '2\u00a0комнаты в\u00a0помещении' },
            { text: '2020\u00a0год постройки, стройка заморожена' },
            { text: 'Санузел совмещённый' },
            { text: 'На полу ламинат' },
            { text: 'Евроремонт' },
            { text: 'Окна во\u00a0двор' }
        ];
        const description = getGeneralDescription(offerWithBuildingState);

        expect(description).toEqual(expectedResult);
    });
});
