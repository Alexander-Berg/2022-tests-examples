const getSeo = require('./getSeo');

describe('mag-article', () => {
    const mag = {
        articlePage: {
            title: 'Главная новинка 2021 года: её выбираете вы!',
            categories: [ { urlPart: 'newcaroftheyear', title: 'Новинка года', category: '' } ],
            mainImage: {
                sizes: {
                    orig: {
                        height: 2268,
                        path: 'https://images.mds-proxy.test.avto.ru/get-vertis-journal/4080458/IMG_0352.jpg_1637921648422/orig',
                        width: 4032,
                    },
                },
            },
        },
    };

    it('должен сгенерировать каноникл на домен auto.ru', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'mag-article',
                    pageParams: {
                        article_id: 'new-car-of-the-year',
                    },
                },
            },
            mag,
        });

        expect(result).toHaveProperty('canonical', 'https://autoru_frontend.base_domain/new-car-of-the-year/');
    });

    it('должен сгенерировать каноникл на домен mag.auto.ru', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'mag-article',
                    pageParams: {
                        article_id: 'delorean-kotoromu-udalos-dobratsya-do-budushchego-fotopost',
                    },
                },
            },
            mag,
        });

        expect(result).toHaveProperty(
            'canonical',
            'https://mag.autoru_frontend.base_domain/article/delorean-kotoromu-udalos-dobratsya-do-budushchego-fotopost/',
        );
    });

    it('amphtml должен отсутствовать', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'mag-article',
                    pageParams: {
                        article_id: 'new-car-of-the-year',
                    },
                },
            },
            mag,
        });

        expect(result).toHaveProperty('amphtml', false);
    });

    it('должен сгенерировать amphtml', () => {
        const result = getSeo({
            config: {
                data: {
                    pageType: 'mag-article',
                    pageParams: {
                        article_id: 'delorean-kotoromu-udalos-dobratsya-do-budushchego-fotopost',
                    },
                },
            },
            mag,
        });

        expect(result).toHaveProperty(
            'amphtml',
            'https://mag.autoru_frontend.base_domain/amp/article/delorean-kotoromu-udalos-dobratsya-do-budushchego-fotopost/',
        );
    });
});
