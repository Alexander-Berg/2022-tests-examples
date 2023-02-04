/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const CallsListingItemTagsSuggest = require('./CallsListingItemTagsSuggest');

const callMock = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock').callsList.calls[0];

const baseProps = {
    call: callMock,
    getTags: _.noop,
};

it('должен отмечать тег иконкой checked, если он есть в списке', () => {
    const callMockClone = _.cloneDeep(callMock);
    callMockClone.tags = [
        { value: 'tag_1' },
        { value: 'tag_1' },
        { value: 'tag_2' },
    ];

    const tree = shallow(
        <CallsListingItemTagsSuggest { ...baseProps } call={ callMockClone }/>,
    );

    const item = tree.instance().renderSuggestItemContent({ value: 'tag_1' });

    expect(item).toMatchSnapshot();
});

describe('https://st.yandex-team.ru/AUTORUFRONT-19239', () => {
    it('должен устанавливать preventClosingPopup в true для работы клика по addNewTag', () => {
        const callMockClone = _.cloneDeep(callMock);
        callMockClone.tags = [];

        const tree = shallow(
            <CallsListingItemTagsSuggest { ...baseProps } call={ callMockClone }/>,
        );
        tree.instance().state.suggestData = [];
        tree.instance().getSuggestData();
        expect(tree.instance().state.preventClosingPopup).toBe(true);
    });

    it('должен вызвать doSelect по клику Enter, если тег существует', () => {
        const callMockClone = _.cloneDeep(callMock);
        callMockClone.tags = [ { value: 'tag_1' } ];

        const tree = shallow(
            <CallsListingItemTagsSuggest { ...baseProps } call={ callMockClone }/>,
        );
        tree.instance().state.suggestData = callMockClone.tags;
        tree.instance().state.inputValue = 'tag_1';
        tree.instance().doSelect = jest.fn();
        tree.instance().onEnterPressed();
        expect(tree.instance().doSelect).toHaveBeenCalledWith({ value: 'tag_1' });
    });

    it('должен вызвать addNewTag по клику Enter, если не тег существует', () => {
        const callMockClone = _.cloneDeep(callMock);
        callMockClone.tags = [ ];

        const tree = shallow(
            <CallsListingItemTagsSuggest { ...baseProps } call={ callMockClone }/>,
        );

        tree.instance().state.inputValue = 'tag_1';
        tree.instance().addNewTag = jest.fn();
        tree.instance().onEnterPressed();
        expect(tree.instance().addNewTag).toHaveBeenCalled();
    });
});
