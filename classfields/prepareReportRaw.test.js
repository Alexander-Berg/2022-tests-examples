const prepareReportRaw = require('./prepareReportRaw').default;

describe('заголовки содержания отчёта', () => {
    describe('vehicle', () => {
        it('должен убрать блок на карточке', () => {
            const result = prepareReportRaw({
                report: {
                    vehicle_options: {},
                    vehicle: {
                        status: 'OK',
                    },
                },
            }, true);

            expect(result.report.content.items.find(item => item.type === 'vehicle')).toBeUndefined();
        });
    });

    describe('отзывы, характеристики, hd фотки', () => {
        it('добавит пункты в превью на карточке', () => {
            const data = {
                report: {
                    report_type: 'FREE_REPORT',
                },
                billing: {
                    service_prices: [
                        { counter: '1', price: 1 },
                    ],
                },
            };
            const result = prepareReportRaw(data, true);

            expect(result.report.content).toMatchObject({
                items: [
                    {
                        key: 'HD фотографии',
                        type: 'hd_photos',
                    },
                    {
                        key: 'Характеристики',
                        type: 'tech_info',
                    },
                    {
                        key: 'Отзывы и рейтинг',
                        type: 'all_reviews',
                    },
                ],
            });
        });

        it('не добавит на странице отчета', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'FREE_REPORT',
                },
                billing: {
                    service_prices: [
                        { price: 1 },
                    ],
                },
            }, false);

            expect(result.report.content).toMatchObject({
                items: [],
            });
        });

        it('не добавит если отчет уже куплен', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'FREE_REPORT',
                },
                billing: {},
            }, true);

            expect(result.report.content).toMatchObject({
                items: [],
            });
        });

        it('добавит количество HD фоток если отчет уже куплен, и есть только HD фото из офферов', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'PAID_REPORT',
                    autoru_offers: {
                        offers: [
                            {
                                photo: { sizes: { orig: '***' } },
                                photos: Array(3).fill({ sizes: { orig: '***' } }),
                            },
                            {
                                photos: Array(5).fill({ sizes: { orig: '***' } }),
                            },
                        ],
                    },
                },
                billing: {},
            }, true);

            expect(result.report.content.items[0]).toMatchObject({
                key: '9 HD фотографий',
                status: 'OK',
                type: 'hd_photos',
                associatedBlockType: 'autoru_offers',
            });
        });

        it('добавит количество HD фоток если отчет уже куплен, и есть только HD фотки автолюбителей', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'PAID_REPORT',
                    vehicle_photos: {
                        records: [
                            {
                                gallery: Array(5).fill({ sizes: { orig: '***' } }),
                            },
                        ],
                    },
                },
                billing: {},
            }, true);

            expect(result.report.content.items[0]).toMatchObject({
                key: '5 HD фотографий',
                status: 'OK',
                type: 'hd_photos',
                associatedBlockType: 'vehicle_photos',
            });
        });

        it('добавит количество HD фоток если отчет уже куплен, есть HD фото у автолюбителей и у офферов', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'PAID_REPORT',
                    autoru_offers: {
                        offers: [
                            {
                                photo: { sizes: { orig: '***' } },
                                photos: Array(3).fill({ sizes: { orig: '***' } }),
                            },
                            {
                                photos: Array(5).fill({ sizes: { orig: '***' } }),
                            },
                        ],
                    },
                    vehicle_photos: {
                        records: [
                            {
                                gallery: Array(5).fill({ sizes: { orig: '***' } }),
                            },
                        ],
                    },
                },
                billing: {},
            }, true);

            expect(result.report.content.items[0]).toMatchObject({
                key: '14 HD фотографий',
                status: 'OK',
                type: 'hd_photos',
                associatedBlockType: 'autoru_offers',
            });
        });

        it('не добавит количество HD фоток если отчет уже куплен, но нет HD фото у автолюбителей или у офферов', () => {
            const result = prepareReportRaw({
                report: {
                    report_type: 'PAID_REPORT',
                },
                billing: {},
            }, true);

            expect(result.report.content).toMatchObject({
                items: [],
            });
        });
    });

    describe('новый блок, к которому никто не готовил', () => {
        it('не попадает в содержание', () => {
            const result = prepareReportRaw({
                report: {
                    hui: {
                        status: 'OK',
                        title: 'Хуй',
                    },
                },
            });

            expect(result.report.content).toMatchObject({
                items: [],
            });
        });
    });
});
