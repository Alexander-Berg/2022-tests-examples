const React = require('react');

const { shallow } = require('enzyme');

const ReviewsList = require('./ReviewsList');

const { journalArticleMock, journalArticlesMock } = require('auto-core/react/dataDomain/journalArticles/mocks');
const { Category } = require('auto-core/react/dataDomain/journalArticles/types');
const getJournalArticles = require('auto-core/react/dataDomain/journalArticles/selectors/getJournalArticles').default;

const articlesMock = journalArticlesMock.withArticles([
    journalArticleMock.withCategory(Category.TESTDRIVES).value(),
    journalArticleMock.withCategory(Category.EXPLAIN).value(),
]).value();

const testdrives = getJournalArticles({ journalArticles: articlesMock }, Category.TESTDRIVES);
const explain = getJournalArticles({ journalArticles: articlesMock }, Category.EXPLAIN);

describe('отрендерит сниппет журнала в правильных местах', () => {

    describe('количество отзывов: 4', () => {
        const reviewsCount = 4;
        it('статьи: тесты, разбор', () => {
            const wrapper = shallowRenderReviewsList(makeReviews(reviewsCount),
                { testdrives, explain });

            const firstArticle = wrapper.children().at(2);
            expect(firstArticle.name()).toBe('JournalSnippet');
            expect(firstArticle.prop('category')).toBe('testdrives');

            const secondArticle = wrapper.children().at(5);
            expect(secondArticle.name()).toBe('JournalSnippet');
            expect(secondArticle.prop('category')).toBe('explain');
        });

        it('статьи: разбор', () => {
            const wrapper = shallowRenderReviewsList(makeReviews(reviewsCount),
                { testdrives: [], explain });

            const article = wrapper.children().at(2);
            expect(article.name()).toBe('JournalSnippet');
            expect(article.prop('category')).toBe('explain');
        });
    });

    describe('количество отзывов: 0', () => {
        it('статьи: тесты, разбор', () => {
            const wrapper = shallowRenderReviewsList([],
                { testdrives, explain });

            const firstArticle = wrapper.children().at(0);
            expect(firstArticle.name()).toBe('JournalSnippet');
            expect(firstArticle.prop('category')).toBe('testdrives');

            const secondArticle = wrapper.children().at(1);
            expect(secondArticle.name()).toBe('JournalSnippet');
            expect(secondArticle.prop('category')).toBe('explain');
        });

        it('статьи: отсутствуют', () => {
            const wrapper = shallowRenderReviewsList([],
                { testdrives: [], explain: [] });
            expect(wrapper.children()).toHaveLength(0);
        });
    });

});

function makeReviews(length) {
    const reviewList = [];
    for (let i = 0; i <= length; i++) {
        reviewList.push({ id: i });
    }
    return reviewList;
}

function shallowRenderReviewsList(reviews, journalArticles) {
    return shallow(
        <ReviewsList
            reviews={ reviews }
            journalArticles={ journalArticles }
            initialPageNum={ 1 }
            searchParams={{ category: 'cars' }}
        />,
    );
}
