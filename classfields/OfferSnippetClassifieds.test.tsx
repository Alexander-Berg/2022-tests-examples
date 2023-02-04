import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import classifiedsMock from 'www-cabinet/react/dataDomain/sales/mock/multipostingClassifieds';

import OfferSnippetClassifieds from './OfferSnippetClassifieds';

it('если у классифайда есть баны, поставит на название классифайда нужный класс и добавит иконку', () => {
    const tree = shallowRenderComponent();

    const bannedClassifiedTitle = tree.find('.OfferSnippetClassifieds__itemHead_banned');

    expect(bannedClassifiedTitle).toExist();
    expect(bannedClassifiedTitle.find('IconSvg')).toExist();
});

it('если классифайд не подключен, покажет кнопку подключения', () => {
    const tree = shallowRenderComponent();
    const emptyClassifiedBlock = tree.find('.OfferSnippetClassifieds__item').at(4);
    expect(emptyClassifiedBlock.find('Link').dive().text()).toBe('Подключить');
});

it('сформирует правильный тултип для статистики звонков', () => {
    const tree = shallowRenderComponent();

    expect(
        tree.find('.OfferSnippetClassifieds__item_avito')
            .find('HoveredTooltip')
            .findWhere(node => node.key() === 'phone_views')
            .prop('tooltipContent'),
    ).toMatchSnapshot();
});

function shallowRenderComponent(classifieds = classifiedsMock) {
    const page = shallow(
        <OfferSnippetClassifieds
            classifieds={ classifieds }
            onClassifiedUpdate={ () => {} }
            isSaleNeedActivation={ false }
        />,
    );
    return page;
}
