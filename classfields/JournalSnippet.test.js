const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const JournalSnippet = require('./JournalSnippet');
const Link = require('auto-core/react/components/islands/Link');

const { journalArticleMock } = require('auto-core/react/dataDomain/journalArticles/mocks');

const journalArticle = journalArticleMock.value();

const context = contextMock;

describe('отрендерит себя', () => {
    it('с категорией "Тест-драйв"', () => {
        const snippet = renderJournalSnippet('testdrives');
        expect(shallowToJson(snippet)).toMatchSnapshot();
    });

    it('с категорией "Разбор"', () => {
        const snippet = renderJournalSnippet('explain');
        expect(shallowToJson(snippet)).toMatchSnapshot();
    });
});

describe('сообщит метрике', () => {
    let snippet;
    beforeEach(() => {
        snippet = renderJournalSnippet('testdrives');
    });

    it('о показе', () => {
        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'car', 'testdrives', 'shows' ]);
    });

    it('о клике по ссылке', () => {
        snippet.find(Link).simulate('click');
        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'car', 'testdrives', 'clicks' ]);
    });
});

function renderJournalSnippet(category) {
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <JournalSnippet article={ journalArticle } category={ category } metrikaPrefix="car"/>
        </ContextProvider>,
    ).dive();
}
