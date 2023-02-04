export const fixtures = {
    'TextToLinksReplacer Метод replaceManyBlocks Корректно заменяет текст на ссылки В нескольких разных блоках': {
        blocks: [
            {
                type: 'text',
                text:
                    '<p>Toyota может\n' +
                    'пополнить свою линейку заряженных GR-моделей седаном. Как <a href="https://www.motortrend.com/news/toyota-gr-camry-corolla-sedan-future-development/" target="_blank">пишет</a> издание Motor Trend со ссылкой на заявление\n' +
                    'вице-президента американского подразделения Toyota Боба\n' +
                    'Картера, компания действительно оценивает перспективы спортивной четырёхдверки.\n' +
                    'При этом базой для неё может стать актуальная Toyota Camry.</p>\n',
            },
            {
                type: 'card',
                card: {
                    text:
                        '<p >На Авто.ру <a href="https://auto.ru/cars/used/sale/kia/rio/1115194777-35b0450d/" target="_blank">опубликовано\n' +
                        '</a>объявление о продаже «капсулы времени» — пятидверной версии Kia Rio выпуска\n' +
                        '2015 года. Пробег автомобиля составляет всего 3877 километров, то есть хэтчбек\n' +
                        'проезжал немногим более полутысячи километров в год. Цена за такую Rio — 965&nbsp;000 рублей.</p>\n',
                },
            },
            {
                type: 'bubble',
                bubble: {
                    content:
                        '<p>Европейское подразделение\n' +
                        'Ford планирует завершить производство семейства Focus на заводе в Саарлуисе в\n' +
                        '2025 году. Как <a href="https://europe.autonews.com/automakers/ford-end-car-production-german-plant" target="_blank">заявил </a>глава подразделения Стюарт Роули изданию Automotive News,\n' +
                        'связано это с переориентацией компании на выпуск электрокаров.</p>\n',
                },
            },
        ],
    },
};
