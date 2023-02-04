export const apartment = {
    offerCategory: 'APARTMENT',
    house: {
        bathroomUnit: 'MATCHED',
        balconyType: 'BALCONY__TWO_LOGGIA',
        windowView: 'YARD'
    },
    apartment: {
        improvements: {
            PHONE: true,
            INTERNET: true,
            ROOM_FURNITURE: true,
            KITCHEN_FURNITURE: true,
            TELEVISION: true,
            WASHING_MACHINE: true,
            REFRIGERATOR: true,
            AIRCONDITION: true,
            BUILD_IN_TECH: true,
            DISHWASHER: true,
            FLAT_ALARM: true
        }
    },
    flootCovering: 'LINOLEUM'
};

export const room = {
    offerCategory: 'ROOMS',
    house: {
        bathroomUnit: 'SEPARATED',
        balconyType: 'LOGGIA'
    },
    apartment: {
        improvements: {
            PHONE: true,
            INTERNET: false,
            ROOM_FURNITURE: true,
            KITCHEN_FURNITURE: true,
            TELEVISION: true,
            WASHING_MACHINE: false,
            REFRIGERATOR: true,
            AIRCONDITION: true,
            BUILD_IN_TECH: false,
            DISHWASHER: true,
            FLAT_ALAR: true
        }
    },
    flootCovering: 'LINOLEUM'
};

export const house = {
    offerCategory: 'HOUSE',
    house: {
        bathroomUnit: 'MATCHED',
        balconyType: 'BALCONY__TWO_LOGGIA',
        pmg: true,
        toilet: 'OUTSIDE',
        shower: 'OUTSIDE',
        improvements: {
            KITCHEN: true,
            POOL: true,
            BILLIARD: true,
            SAUNA: true
        }
    },
    building: {
        buildingType: 'BRICK'
    },
    apartment: {
        renovation: 'COSMETIC_DONE'
    },
    supplyMap: {
        HEATING: true,
        WATER: true,
        SEWERAGE: true,
        ELECTRICITY: true,
        GAS: true
    },
    flootCovering: 'LINOLEUM'
};

export const housePart = {
    offerCategory: 'HOUSE_PART',
    house: {
        bathroomUnit: 'SEPARATED',
        balconyType: 'TWO_BALCONY',
        pmg: false,
        toilet: 'INSIDE',
        shower: 'INSIDE',
        improvements: {
            KITCHEN: false,
            POOL: false,
            BILLIARD: false,
            SAUNA: false
        }
    },
    apartment: {
        renovation: 'COSMETIC_DONE'
    },
    building: {
        buildingType: 'WOOD'
    },
    supplyMap: {
        HEATING: false,
        WATER: false,
        SEWERAGE: false,
        ELECTRICITY: false,
        GAS: false
    },
    flootCovering: 'LINOLEUM'
};

export const lot = {
    offerCategory: 'LOT',
    toilet: 'OUTSIDE',
    shower: 'OUTSIDE',
    house: {
        pmg: true
    },
    supplyMap: {
        HEATING: true,
        WATER: true,
        SEWERAGE: true,
        ELECTRICITY: true,
        GAS: true
    }
};

export const garage = {
    offerCategory: 'GARAGE',
    garage: {
        garageName: 'БСК',
        ownershipType: 'PRIVATE',
        improvements: {
            AUTOMATIC_GATES: true,
            INSPECTION_PIT: true,
            CELLAR: true,
            CAR_WASH: true,
            AUTO_REPAIR: true
        }
    },
    supplyMap: {
        WATER: true,
        HEATING: true,
        ELECTRICITY: true
    },
    building: {
        parkingType: 'NEARBY',
        buildingType: 'METAL',
        buildingImprovementsMap: {
            CCTV: true,
            TWENTY_FOUR_SEVEN: true,
            SECURITY: true,
            ACCESS_CONTROL_SYSTEM: true
        }
    },
    apartment: {
        improvements: {
            FIRE_ALARM: true
        }
    }
};

export const commercial = {
    offerCategory: 'COMMERCIAL',
    hasAlarm: true,
    building: {
        siteDisplayName: 'Сенатор',
        officeClass: 'A+',
        parkingPlaces: 5,
        parkingGuestPlaces: 10,
        parkingPlacePrice: '800 р/мес',
        builtYear: '1989',
        buildingType: 'WOOD',
        buildingSeries: '25 ЖС',
        buildingImprovementsMap: {
            ELITE: true,
            SECURITY: true,
            TWENTY_FOUR_SEVEN: true,
            ACCESS_CONTROL_SYSTEM: true,
            PARKING: true,
            EATING_FACILITIES: true,
            LIFT: true,
            PARKING_GUEST: true,
            RUBBISH_CHUTE: true
        }
    },
    apartment: {
        quality: 'GOOD',
        renovation: 'GOOD',
        improvements: {
            INTERNET: true,
            AIRCONDITION: true,
            FIRE_ALARM: true,
            ROOM_FURNITURE: true,
            SELF_SELECTION_TELECOM: true,
            ADDING_PHONE_ON_REQUEST: true,
            VENTILATION: true,
            RESPONSIBLE_STORAGE: true
        }
    },
    flootCovering: 'PARQUET',
    house: {
        windowView: 'STREET',
        windowType: 'DISPLAY',
        entranceType: 'SEPARATE',
        phone_lines: 6,
        electricCapacity: 600
    },
    supplyMap: {
        ELECTRICITY: false,
        SEWERAGE: false,
        HEATING: false,
        WATER: false,
        GAS: false
    },
    commercial: {
        temperatureComment: '25 градусов',
        palletPrice: '400 р/мес',
        improvements: {
            SERVICE_THREE_PL: true,
            FREIGHT_ELEVATOR: true,
            OFFICE_WAREHOUSE: true,
            TRUCK_ENTRANCE: true,
            OPEN_AREA: true,
            RAILWAY: true,
            RAMP: true
        }
    }
};
