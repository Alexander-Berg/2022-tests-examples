const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const JournalSnippet = require('./JournalSnippet');
const Link = require('auto-core/react/components/islands/Link');

const { journalArticleMock } = require('auto-core/react/dataDomain/journalArticles/mocks');

const journalArticle = journalArticleMock.withTags([
    {
        urlPart: 'mercedes',
        title: 'Mercedes-Benz',
    },
    {
        urlPart: 'mercedes-gklasseamg',
        title: 'G-Класс AMG',
    },
]).value();

const context = contextMock;

const carMock = {
    mark: 'MERCEDES',
    model: 'G_KLASSE_AMG',
};

const carMockWithCatalogFilter = {
    catalog_filter: [
        { mark: 'MERCEDES', model: 'G_KLASSE_AMG', generation: '111' },
        { mark: 'MERCEDES', model: 'G_KLASSE_AMG', generation: '222' },
    ],
};

describe('отрендерит себя', () => {
    it('с категорией "Тест-драйв"', () => {
        const snippet = renderJournalSnippet('testdrives', carMock);
        expect(shallowToJson(snippet)).toMatchSnapshot();
    });

    it('с категорией "Разбор"', () => {
        const snippet = renderJournalSnippet('explain', carMock);
        expect(shallowToJson(snippet)).toMatchSnapshot();
    });

    it('на странице с мультивыбором', () => {
        const snippet = renderJournalSnippet('explain', carMockWithCatalogFilter);
        expect(shallowToJson(snippet)).toMatchSnapshot();
    });
});

describe('сообщит метрике', () => {
    let snippet;
    beforeEach(() => {
        snippet = renderJournalSnippet('testdrives', carMock);
    });

    it('о показе', () => {
        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'car', 'testdrives', 'shows' ]);

    });

    it('о клике по ссылке', () => {
        snippet.find(Link).simulate('click');
        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'car', 'testdrives', 'clicks' ]);
    });
});

function renderJournalSnippet(category, car) {
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <JournalSnippet article={ journalArticle } category={ category } car={ car } metrikaPrefix="car"/>
        </ContextProvider>,
    ).dive();
}
