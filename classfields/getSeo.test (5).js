const getSeo = require('./getSeo');

it('should return seo for form', () => {
    expect(getSeo({
        config: {
            data: {
                pageType: 'form-add',
            },
        },
    })).toEqual({
        title: 'Размещение объявления',
    });
});

describe('form-cars-evaluation', () => {

    it('should return seo for empty page', () => {
        expect(getSeo({
            config: {
                data: {
                    pageType: 'form-cars-evaluation',
                },
            },
        })).toEqual({
            canonical: 'https://autoru_frontend.base_domain/cars/evaluation/',
            description: 'Сервис оценки автомобилей на Авто.ру. ' +
            'Укажите параметры своей машины и узнайте среднюю цену вашего авто на рынке. ' +
            'Онлайн-оценка автомобиля на Авто.ру для легковых машин с пробегом',
            h1: 'Сколько стоит автомобиль?',
            title: 'Сервис оценки автомобиля, онлайн-оценка машины на Авто.ру',
            ogTitle: 'Сервис оценки автомобиля, онлайн-оценка машины на Авто.ру',
        });
    });

    it('should return seo share page', () => {
        expect(getSeo({
            config: {
                data: {
                    pageType: 'form-cars-evaluation',
                },
            },
            evaluation: {
                data: {
                    offer: {
                        label_mark_model: 'Hyundai ix35',
                        year: 2015,
                    },
                },
            },
        })).toEqual({
            canonical: 'https://autoru_frontend.base_domain/cars/evaluation/',
            description: 'Сервис оценки автомобилей на Авто.ру. ' +
            'Укажите параметры своей машины и узнайте среднюю цену вашего авто на рынке. ' +
            'Онлайн-оценка автомобиля на Авто.ру для легковых машин с пробегом',
            h1: 'Сколько стоит автомобиль?',
            title: 'Правильная цена от Авто.ру на Hyundai ix35 2015 года',
            ogTitle: 'Смотрите правильную цену от Авто.ру на Hyundai ix35 2015 года',
        });
    });

});
