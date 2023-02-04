const React = require('react');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const Context = createContextProvider(contextMock);

const TagsLinks = require('./TagsLinks');

TagsLinks.TAGS_PARAMS.forEach(({ name }) => {
    it('Рендерит блок TagsLinks со ссылкой на search_tag ' + name, () => {
        const tree = shallow(
            <Context>
                <TagsLinks/>
            </Context >,
        ).dive();
        const linkArr = tree.find('.TagsLinks__linkWrapper');

        expect(linkArr.findWhere(node => node.key() === 'TagsLinks_' + name)).toExist();
    });
});

TagsLinks.CUSTOM_TAGS_PARAMS.forEach(({ name }) => {
    it('Рендерит блок TagsLinks со ссылкой на custom_tag ' + name, () => {
        const tree = shallow(
            <Context>
                <TagsLinks/>
            </Context >,
        ).dive();
        const linkArr = tree.find('.TagsLinks__linkWrapper');

        expect(linkArr.findWhere(node => node.key() === 'TagsLinks_' + name)).toExist();
    });
});

it('Рендерит блок TagsLinks со ссылкой на тэг новинок', () => {
    const tree = shallow(
        <Context>
            <TagsLinks/>
        </Context >,
    ).dive();
    const linkArr = tree.find('.TagsLinks__linkWrapper');

    expect(linkArr.findWhere(node => node.key() === 'TagsLinks_new4new')).toExist();
});
