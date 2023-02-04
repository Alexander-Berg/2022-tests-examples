export const finishedSite = {
    id: 1,
    name: 'Элитная новостройка',
    fullName: 'ЖК "Элитная новостройка"',
    price: {
        currency: 'RUR',
        minPricePerMeter: 265805
    },
    flatStatus: 'ON_SALE',
    viewTypes: [ 'GENERAL' ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, Ореховый бул., вл. 24, к. 2',
        subjectFederationRgid: 1
    },
    images: [ 'http://site.com/image.png' ],
    appLargeImages: [ 'http://site.com/image.png' ],
    developers: [],
    siteSpecialProposals: [ ],
    state: 'HAND_OVER',
    withBilling: true,
    awards: {}
};

export const unfinishedSite = {
    id: 2,
    name: 'Новостройка бизнес класса с очень длинным названием',
    fullName: 'ЖК "Новостройка бизнес класса"',
    price: {},
    flatStatus: 'SOLD',
    viewTypes: [ ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, ул. Золоторожский Вал, 11, стр. 20',
        subjectFederationRgid: 1
    },
    images: [ ],
    appLargeImages: [ ],
    developers: [],
    siteSpecialProposals: [],
    buildingClass: 'BUSINESS',
    state: 'UNFINISHED',
    withBilling: true,
    awards: {}
};

export const suspendedSite = {
    id: 3,
    name: 'Комфортная новостройка',
    fullName: 'ЖК "Элитная Комфортная"',
    price: {
        currency: 'RUR',
        minPricePerMeter: 166515
    },
    flatStatus: 'ON_SALE',
    viewTypes: [ 'GENERAL' ],
    salesDepartment: {
        name: 'Офис'
    },
    location: {
        address: 'Москва, Шелепихинская наб., вл. 34',
        subjectFederationRgid: 1
    },
    images: [ 'http://site.com/image.png' ],
    appLargeImages: [ 'http://site.com/image.png' ],
    developers: [],
    siteSpecialProposals: [],
    buildingClass: 'COMFORT',
    state: 'CONSTRUCTION_SUSPENDED',
    withBilling: true,
    awards: {}
};
