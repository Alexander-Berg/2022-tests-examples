const reviewsUrlsBuilder = require('./reviewsUrlsBuilder');
const { REVIEW_DEMO_PARAMS } = require('../lib/constants');

it('Возвращает все возможные ссылки отзывов по параметрам', () => {
    expect(reviewsUrlsBuilder(REVIEW_DEMO_PARAMS)).toMatchSnapshot();
});
