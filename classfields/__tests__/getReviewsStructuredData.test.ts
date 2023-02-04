import { MOCK_REVIEWS } from 'realty-core/view/react/common/components/Microdata/__tests__/mock';
import { IReviewEntry } from 'realty-core/types/reviews';

import { getReviewsData } from '../../getReviewsData';

describe('getReviewsStructuredData', () => {
    it('возвращает пустой массив, когда нет нужных данных', () => {
        expect(getReviewsData([])).toEqual([]);
    });

    it('возвращает объект с отзывами в соотвествии со схемой разметки для отзывов', () => {
        const reviews = MOCK_REVIEWS.entries as IReviewEntry[];
        const result = [
            {
                '@type': 'Review',
                author: {
                    '@type': 'Person',
                    name: 'Vasya Pupkin',
                },
                datePublished: '2020-08-04T15:14:35.498Z',
                name: 'Отзывы о ЖК',
                reviewBody: 'Мне прям очень понравилось',
                reviewRating: {
                    '@type': 'Rating',
                    ratingValue: 5,
                },
            },
            {
                '@type': 'Review',
                author: {
                    '@type': 'Person',
                    name: 'Vasya Ivanov',
                },
                datePublished: '2020-04-04T15:14:35.498Z',
                name: 'Отзывы о ЖК',
                reviewBody: 'Очень плохой отзыв!',
                reviewRating: {
                    '@type': 'Rating',
                    ratingValue: 1,
                },
            },
        ];

        expect(getReviewsData(reviews)).toEqual(result);
    });
});
