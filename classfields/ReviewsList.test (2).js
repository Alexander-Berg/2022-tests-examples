const React = require('react');

const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const { journalArticleMock, journalArticlesMock } = require('auto-core/react/dataDomain/journalArticles/mocks');
const { Category } = require('auto-core/react/dataDomain/journalArticles/types');
const getJournalArticles = require('auto-core/react/dataDomain/journalArticles/selectors/getJournalArticles').default;

const ReviewsList = require('./ReviewsList');

const articlesMock = journalArticlesMock.withArticles([
    journalArticleMock.withCategory(Category.TESTDRIVES).value(),
    journalArticleMock.withCategory(Category.EXPLAIN).value(),
]).value();

const testdrives = getJournalArticles({ journalArticles: articlesMock }, Category.TESTDRIVES);
const explain = getJournalArticles({ journalArticles: articlesMock }, Category.EXPLAIN);
const props = {
    pager: {
        page_size: 10,
    },
    params: {},
};

describe('отрендерит виджет проверки и сниппет журнала в правильных местах', () => {
    /*
     * 0  | 1         | 2  | 3           | 4  | 5  | 6  | 7
     * R1 | VinWidget | R2 | JournalTest | R3 | AD | R4 | JournalExplain
     */
    describe('количество отзывов: 4', () => {
        const reviewsCount = 4;

        it('статьи: тесты, разбор', () => {
            const wrapper = shallowRenderReviewsList(makeReviewsList(reviewsCount),
                { testdrives, explain })
                .find('.ReviewsList__chunk');

            const vinWidget = wrapper.children().at(1);
            expect(vinWidget.name()).toContain('VinWidget');

            const firstArticle = wrapper.children().at(3);
            expect(firstArticle.name()).toBe('JournalSnippet');
            expect(firstArticle.prop('category')).toBe('testdrives');

            const secondArticle = wrapper.children().at(7);
            expect(secondArticle.name()).toBe('JournalSnippet');
            expect(secondArticle.prop('category')).toBe('explain');
        });

        it('статьи: разбор', () => {
            const wrapper = shallowRenderReviewsList(makeReviewsList(reviewsCount),
                { testdrives: [], explain })
                .find('.ReviewsList__chunk');

            const article = wrapper.children().at(3);
            expect(article.name()).toBe('JournalSnippet');
            expect(article.prop('category')).toBe('explain');
        });
    });

    describe('количество отзывов: 0', () => {
        it('статьи: тесты, разбор', () => {
            const wrapper = shallowRenderReviewsList([],
                { testdrives, explain })
                .find('.ReviewsList__chunk');

            const firstArticle = wrapper.children().at(0);
            expect(firstArticle.name()).toBe('JournalSnippet');
            expect(firstArticle.prop('category')).toBe('testdrives');

            const secondArticle = wrapper.children().at(1);
            expect(secondArticle.name()).toBe('JournalSnippet');
            expect(secondArticle.prop('category')).toBe('explain');
        });

        it('статьи: отсутствуют', () => {
            const wrapper = shallowRenderReviewsList([],
                { testdrives: [], explain: [] })
                .find('.ReviewsList__chunk');
            expect(wrapper.children()).toHaveLength(0);
        });
    });

});

function makeReviewsList(length) {
    const reviewList = [];
    for (let i = 0; i < length; i++) {
        reviewList.push({ id: i.toString() });
    }
    return reviewList;
}

function shallowRenderReviewsList(reviews, journalArticles) {
    const ContextProvider = createContextProvider(contextMock);
    return shallow(
        <ContextProvider>
            <ReviewsList
                { ...props }
                reviews={ reviews }
                journalArticles={ journalArticles }
            />
        </ContextProvider>,
    ).dive();
}
