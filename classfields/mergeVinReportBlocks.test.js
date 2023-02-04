const de = require('descript');

const mergeVinReportBlocks = require('./mergeVinReportBlocks');

it('не должен ничего вернуть, если нет отчёта', () => {
    const result = mergeVinReportBlocks({ vinReport: { error: '' } });
    expect(result).toBeUndefined();
});

describe('all reviews', () => {
    let mock;
    beforeEach(() => {
        mock = {
            reviewsSummary: {
                averageRating: 4.5,
                totalCount: 1,
            },
            reviewsFeatures: {
                foo: 'bar',
            },
            vinReport: {
                report: {
                    car_info: {
                        mark_info: { code: 'AUDI' },
                        model_info: { code: 'A4' },
                        super_gen: { id: '1' },
                    },
                    content: {
                        items: [],
                    },
                },
            },
        };
    });

    it('должен смержить отзывы, если есть', () => {
        const result = mergeVinReportBlocks(mock);

        expect(result).toMatchObject({
            vinReport: {
                report: {
                    content: {
                        items: [
                            {
                                key: 'Отзывы и рейтинг',
                                status: 'OK',
                                type: 'all_reviews',
                                value: '4,5 баллов из 5',
                            },
                        ],
                    },
                    all_reviews: {
                        header: { title: 'Отзывы и рейтинг модели' },
                        summary: {
                            averageRating: 4.5,
                        },
                        features: {
                            foo: 'bar',
                        },
                        resourceParams: {
                            category: 'cars',
                            mark: 'AUDI',
                            model: 'A4',
                            super_gen: '1',
                        },
                    },
                },
            },
        });
    });

    it('не должен смержить отзывы, если они ответили, но отзывов нет', () => {
        mock.reviewsSummary.totalCount = 0;
        const result = mergeVinReportBlocks(mock);

        expect(result.vinReport.report.all_reviews).toBeUndefined();
    });

    it('не должен смержить отзывы, если есть summary, но нет features', () => {
        mock.reviewsSummary = de.error('BLOCK_FAILED');
        const result = mergeVinReportBlocks(mock);

        expect(result.vinReport.report.all_reviews).toBeUndefined();
    });

    it('не должен смержить отзывы, если есть features, но нет summary', () => {
        mock.reviewsFeatures = de.error('BLOCK_FAILED');
        const result = mergeVinReportBlocks(mock);

        expect(result.vinReport.report.all_reviews).toBeUndefined();
    });

});

describe('tech_info', () => {
    let mock;
    beforeEach(() => {
        mock = {
            cardTechInfo: {
                data: {
                    foo: 'bar',
                },
            },
            vinReport: {
                report: {
                    content: {
                        items: [],
                    },
                },
            },
        };
    });

    it('должен смержить техпараметры, если есть', () => {
        const result = mergeVinReportBlocks(mock);

        expect(result).toMatchObject({
            vinReport: {
                report: {
                    content: {
                        items: [
                            {
                                key: 'Характеристики',
                                status: 'OK',
                                type: 'tech_info',
                                value: 'Найдены в каталоге',
                            },
                        ],
                    },
                    tech_info: {
                        header: { title: 'Характеристики' },
                        foo: 'bar',
                    },
                },
            },
        });
    });

    it('не должен смержить техпараметры, если их нет', () => {
        mock.cardTechInfo = de.error('BLOCK_FAILED');
        const result = mergeVinReportBlocks(mock);

        expect(result.vinReport.report.tech_info).toBeUndefined();
    });
});
