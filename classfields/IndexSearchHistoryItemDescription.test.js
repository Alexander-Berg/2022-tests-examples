const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const IndexSearchHistoryItemDescription = require('./IndexSearchHistoryItemDescription');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const indexSearchHistoryMock = require('autoru-frontend/mockData/state/indexSearchHistory.mock');

it('должен сам вычислить заголовок, если его нет в ответе бэка', () => {
    const item = _.cloneDeep(indexSearchHistoryMock[0]);
    delete item.title;
    const tree = shallow(
        <IndexSearchHistoryItemDescription
            item={ item }
            index={ 0 }
        />, { context: contextMock },
    );
    expect(tree.find('.IndexSearchHistoryItemDescription__title').children().at(0).text()).toBe('Audi A7 II, A7 I');
});

it('должен взять заголовок из ответа бэка, если он там есть', () => {
    const tree = shallow(
        <IndexSearchHistoryItemDescription
            item={ indexSearchHistoryMock[0] }
            index={ 0 }
        />, { context: contextMock },
    );
    expect(tree.find('.IndexSearchHistoryItemDescription__title').children().at(0).text()).toBe('Заголовок поиска из ответа бэка');
});
