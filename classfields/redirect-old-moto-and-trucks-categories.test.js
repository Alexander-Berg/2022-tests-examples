const redirectOldMotoAndTrucksCategoriesTest = require('./redirect-old-moto-and-trucks-categories');

const TESTS = [
    {
        url: '/legkie-gruzoviki/all/',
        result: '/lcv/all/',
    },
    {
        url: '/trucks/all/',
        result: '/truck/all/',
    },
    {
        url: '/artic/all/',
        result: null,
    },
    {
        url: '/drags/all/',
        result: '/trailer/all/',
    },
    {
        url: '/mototsikly/all/',
        result: '/motorcycle/all/',
    },
    {
        url: '/rossiya/motovezdehody/all/',
        result: '/rossiya/atv/all/',
    },
    {
        url: '/skutery/all/',
        result: '/scooters/all/',
    },
    {
        url: '/snegohody/all/',
        result: '/snowmobile/all/',
    },
    {
        url: '/trucks/all/',
        result: '/truck/all/',
    },
    {
        url: '/drags/used/sale/rosspecpricep/9462/15194022-42eb05d2/',
        result: '/trailer/used/sale/rosspecpricep/9462/15194022-42eb05d2/',
    },
    // не надо редиректить
    {
        url: '/like/trucks/',
        result: null,
    },
    {
        url: '/login/trucks/',
        result: null,
    },
    {
        url: '/my/trucks/',
        result: null,
    },
    {
        url: '/trucks/reviews/add/?from=mobile.reviews',
        result: null,
    },
    {
        url: '/trucks/reviews/edit/?from=mobile.reviews',
        result: null,
    },
    {
        url: '/moskva/crane_hydraulics/all/',
        result: '/moskva/truck/all/',
    },
];

TESTS.forEach(function(testCase) {
    it(`должен вернуть редирект "${ testCase.url }" -> "${ testCase.result }" `, function() {
        expect(redirectOldMotoAndTrucksCategoriesTest(testCase.url)).toEqual(testCase.result);
    });
});
