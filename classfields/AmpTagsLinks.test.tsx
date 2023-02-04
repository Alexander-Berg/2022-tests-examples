import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import TAGS_PARAMS from 'auto-core/data/dicts/seoTags';
import CUSTOM_TAGS_PARAMS from 'auto-core/data/dicts/seoCustomTags';

const Context = createContextProvider(contextMock);

import AmpTagsLinks from './AmpTagsLinks';

TAGS_PARAMS.forEach(({ name }) => {
    it('Рендерит блок AmpTagsLinks со ссылкой на search_tag ' + name, () => {
        const tree = shallow(
            <Context>
                <AmpTagsLinks/>
            </Context >,
        ).dive();
        const linkArr = tree.find('.AmpTagsLinks__linkWrapper');

        expect(linkArr.findWhere(node => node.key() === 'AmpTagsLinks_' + name)).toExist();
    });
});

CUSTOM_TAGS_PARAMS.forEach(({ name }) => {
    it('Рендерит блок AmpTagsLinks со ссылкой на custom_tag ' + name, () => {
        const tree = shallow(
            <Context>
                <AmpTagsLinks/>
            </Context >,
        ).dive();
        const linkArr = tree.find('.AmpTagsLinks__linkWrapper');

        expect(linkArr.findWhere(node => node.key() === 'AmpTagsLinks_' + name)).toExist();
    });
});

it('Рендерит блок AmpTagsLinks со ссылкой на тэг новинок', () => {
    const tree = shallow(
        <Context>
            <AmpTagsLinks/>
        </Context >,
    ).dive();
    const linkArr = tree.find('.AmpTagsLinks__linkWrapper');

    expect(linkArr.findWhere(node => node.key() === 'AmpTagsLinks_new4new')).toExist();
});
