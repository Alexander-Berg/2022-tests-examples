const dealersListingSeo = require('./index');

const mockState = (searchParams, geo, dealers) => {
    return {
        geo,
        breadcrumbs: {
            marks: [
                {
                    'cyrillic-name': 'АС',
                    name: 'AC',
                    val: 'AC',
                },
                {
                    'cyrillic-name': 'Ауди',
                    name: 'Audi',
                    val: 'AUDI',
                },
            ],
        },
        dealersListing: {
            searchParams,
            marks: [
                {
                    count: 227,
                    'cyrillic-name': 'Ауди',
                    name: 'Audi',
                    popular: true,
                    val: 'AUDI',
                },
            ],
            dealers: dealers ? [ {
                dealerId: '20135115',
                dealerName: 'Автофорум Audi',
                dealerCode: 'avtoforum_moskva_audi',
                dealerLogo: '//avatars.mds.yandex.net/get-verba/1030388/2a000001645b2b275708d60908ffb6eee413/dealer_logo',
                dealerLink: '/diler-oficialniy/cars/all/avtoforum_moskva_mercedes/?from=dealer-listing-list',
                isLoyalty: false,
                markId: 'mercedes',
                markName: 'Audi',
                markLogo: '//avatars.mds.yandex.net/get-verba/216201/2a00000164d564c2cb01ec4bc5d82315b68c/logo',
                netId: '20156383',
                netAlias: 'major',
                netName: 'Major',
                orgType: '1',
                address: 'Россия, Москва и Московская область, Москва, внешняя сторона, 92-й километр, МКАД',
                metro: [ { rid: 20385, name: 'Медведково', distance: 2549.6949601436345 }, {
                    rid: 20386,
                    name: 'Бабушкинская',
                    distance: 3578.0624850522795,
                } ],
                phones: { list: [], isFetching: false },
                latitude: 55.894658,
                longitude: 37.700358,
                filteredOffersCount: 225,
            } ] : [],
            totalResultsCount: dealers ? 1 : 0,
        },
    };
};

const seoFields = [ 'description', 'h1', 'title', 'seoText' ];
const prefix = 'Должно быть правильное содержимое ';
const suffixes = {
    MSK: {
        gidsInfo: [
            {
                id: 213,
                linguistics: {
                    ablative: '',
                    accusative: 'Москву',
                    dative: 'Москве',
                    directional: '',
                    genitive: 'Москвы',
                    instrumental: 'Москвой',
                    locative: '',
                    nominative: 'Москва',
                    preposition: 'в',
                    prepositional: 'Москве',
                },
            },
        ],
    },
    NO_REGION: {},
};
const dealers = {
    WITH_DEALERS: true,
    WITHOUT_DEALERS: false,
};

seoFields.forEach(item => {
    for (const dealer in dealers) {
        for (const suffix in suffixes) {
            const geo = suffixes[suffix];
            it(prefix + item + ' для дилеров новых без марки ' + suffix + ' ' + String(dealer), () => {
                const searchParams = {
                    section: 'new',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' для дилеров новых Audi с маркой ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'new',
                    mark: 'audi',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' для дилеров поддержанных авто ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'used',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' для дилерской сети Major новых Audi ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'new',
                    mark: 'audi',
                    dealer_net_semantic_url: 'major',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' для дилерской сети Major новых автомобилей ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'new',
                    dealer_net_semantic_url: 'major',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' для дилерской сети Major любых автомобилей ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'all',
                    dealer_net_semantic_url: 'major',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });

            it(prefix + item + ' любых автомобилей ' + suffix + ' ' + dealer, () => {
                const searchParams = {
                    section: 'all',
                };
                expect(dealersListingSeo[item](mockState(searchParams, geo, dealers[dealer]))).toMatchSnapshot();
            });
        }
    }
});
