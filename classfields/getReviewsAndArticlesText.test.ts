import getReviewsAndArticlesText from './getReviewsAndArticlesText';

describe('getReviewsAndArticlesText', () => {
    it('вернет количество статей и отзывов', () => {
        const actual = getReviewsAndArticlesText({ value: 20 });

        expect(actual).toEqual('статей и отзывов');
    });

    it('вернет статья и отзыв', () => {
        const actual = getReviewsAndArticlesText({ value: 61 });

        expect(actual).toEqual('статья и отзыв');
    });
});
