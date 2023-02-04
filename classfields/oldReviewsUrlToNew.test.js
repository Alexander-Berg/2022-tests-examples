const oldReviewsUrlToNew = require('./oldReviewsUrlToNew');

const urlMaps = [
    // Листинг
    { oldUrl: '', newUrl: '/reviews/' },
    { oldUrl: '/', newUrl: '/reviews/' },
    { oldUrl: '/cars/', newUrl: '/reviews/cars/' },
    { oldUrl: '/cars', newUrl: '/reviews/cars/' },
    { oldUrl: '/moto', newUrl: '/reviews/moto/' },
    { oldUrl: '/commercial', newUrl: '/reviews/trucks/' },
    { oldUrl: '/scooters/suzuki', newUrl: '/reviews/moto/scooters/suzuki/' },
    { oldUrl: '/scooters/suzuki/address', newUrl: '/reviews/moto/scooters/suzuki/address_110/' },
    // Тест маппинга старых id марок и моделей в новые (harley-davidson -> harley_davidson)
    { oldUrl: '/motorcycle/harley-davidson/vrsc/', newUrl: '/reviews/moto/motorcycle/harley_davidson/vrsc/' },
    { oldUrl: '/atv/russkaya-mehanika/ris-500/', newUrl: '/reviews/moto/atv/russkaya_mehanika/ris_500/' },

    { oldUrl: '/cars/mazda/cx-7/opinions/', newUrl: '/reviews/cars/mazda/cx_7/' },
    { oldUrl: '/cars/mazda/cx-7/6447581/opinions/', newUrl: '/reviews/cars/mazda/cx_7/' },
    { oldUrl: '/cars/lada/2106/group-sedan/mod-17227/opinions/', newUrl: '/reviews/cars/vaz/2106/' },
    { oldUrl: '/cars/toyota/land-cruiser-prado/150/group-offroad_5d/mod-16631/opinions/', newUrl: '/reviews/cars/toyota/land_cruiser_prado/' },
    { oldUrl: '/cars/fiat/doblo/i_res/group-compactvan/modification/opinions/mod-52487/', newUrl: '/reviews/cars/fiat/doblo/' },
    { oldUrl: '/cars/audi/a6/ii_res/group-sedan/modification/opinions/mod-89823/', newUrl: '/reviews/cars/audi/a6/' },

    { oldUrl: '/drags/mzsa/81771/', newUrl: '/reviews/trucks/trailer/mzsa/81771/' },
    { oldUrl: '/light_trucks/baw/', newUrl: '/reviews/trucks/lcv/baw/' },
    { oldUrl: '/light_trucks/baw/fenix/', newUrl: '/reviews/trucks/lcv/baw/fenix_lcv1/' },
    { oldUrl: '/trucks/daf/', newUrl: '/reviews/trucks/truck/daf/' },
    { oldUrl: '/trucks/daf/45/', newUrl: '/reviews/trucks/truck/daf/lf_45/' },
    { oldUrl: '/trailers/chayka-servis/', newUrl: '/reviews/trucks/trailer/chayka_servis/' },

    // Листинг, фоллбек с неизвестных категорий
    { oldUrl: '/dredge/eo/', newUrl: '/reviews/cars/' },
    { oldUrl: '/other/', newUrl: '/reviews/cars/' },
    { oldUrl: '/trucktrailers/kupava/', newUrl: '/reviews/cars/' },

    // Карточка отзыва
    { oldUrl: '/review/12345', newUrl: '/review/12345/' },
    { oldUrl: '/review/12345/', newUrl: '/review/12345/' },
    { oldUrl: '/review/61166232.html', newUrl: '/review/61166232/' },
    { oldUrl: '/review/61166232.html/', newUrl: '/review/61166232/' },

    // Форма добавления и Мои отзывы
    { oldUrl: '/add', newUrl: '/cars/reviews/add/' },
    { oldUrl: '/opinions/', newUrl: '/my/reviews/' },
    { oldUrl: '/reviews/edit/61167987/', newUrl: '/my/reviews/' },
    { oldUrl: '/reviews/add/addition/61167987/', newUrl: '/my/reviews/' },
];

urlMaps.forEach((urlMap) => {
    it(`should correctly transform old reviews url (${ urlMap.oldUrl }) to new (${ urlMap.newUrl })`, () => {
        expect(oldReviewsUrlToNew(urlMap.oldUrl)).toEqual(urlMap.newUrl);
    });
});
