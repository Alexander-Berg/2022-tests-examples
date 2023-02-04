import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import _ from 'lodash';

import type { Call } from '@vertis/schema-registry/ts-types-snake/auto/calltracking/model';

import context from 'autoru-frontend/mocks/contextMock';

import callMock from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';

import CallsListingItemTags from './CallsListingItemTags';

const baseProps = {
    call: callMock.callsList.calls[0] as unknown as Call,

    addTag: () => Promise.resolve(),
    removeTag: () => Promise.resolve(),
    getTags: () => Promise.resolve([]),
};

it('должен показывать кнопку "Показать еще", если тегов больше 2', () => {
    const callMockClone = _.cloneDeep(baseProps.call);
    callMockClone.tags = [
        { value: 'tag_1' },
        { value: 'tag_1' },
        { value: 'tag_2' },
    ];

    const tree = shallow(
        <CallsListingItemTags { ...baseProps } call={ callMockClone }/>,
        { context },
    );

    const button = tree.find('.CallsListingItemTags__showAllButton');

    expect(shallowToJson(button)).not.toBeNull();
});

it('должен показывать остальные теги по нажатию кнопки "Показать еще"', () => {
    const callMockClone = _.cloneDeep(baseProps.call);
    callMockClone.tags = [
        { value: 'tag_1' },
        { value: 'tag_1' },
        { value: 'tag_2' },
    ];

    const tree = shallow(
        <CallsListingItemTags { ...baseProps } call={ callMockClone }/>,
        { context },
    );

    tree.find('.CallsListingItemTags__showAllButton').simulate('click');

    const tags = tree.find('.CallsListingItemTags__tag');

    expect(tags).toHaveLength(3);

    const button = tree.find('.CallsListingItemTags__showAllButton');

    expect(shallowToJson(button)).toBeNull();
});
