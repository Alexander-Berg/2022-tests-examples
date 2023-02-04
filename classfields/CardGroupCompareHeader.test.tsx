import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import getAllCatalogComplectations from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/selectors/getAllCatalogComplectations';
import catalogSubtreeMock from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree';
import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';

import CardGroupCompareHeader from './CardGroupCompareHeader';

const complectations = getAllCatalogComplectations({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

const searchParameters = {
    catalog_filter: [ {} ],
};

describe('CardGroupCompareHeader должен отрендерить правильный проп', () => {
    const tree = shallow(
        <CardGroupCompareHeader
            complectations={ complectations }
            visibleItems={ [ 'Style', 'Active' ] }
            onControlClick={ jest.fn() }
            onSortEnd={ jest.fn() }
            onDragMove={ jest.fn() }
            isStart={ true }
            isEnd={ false }
            hasControls={ true }
            closePopup={ jest.fn() }
            isPopupClosed={ true }
            searchParameters={ searchParameters }
        />,
        { context: contextMock },
    );

    it('metrika на ссылке', () => {
        expect(tree.find('Link').first().prop('metrika')).toBe('about_model,options,compare,load_listing');
    });

    it('url на ссылке', () => {
        expect(tree.find('Link').first().prop('url')).toBe('link/card-group/?catalog_filter=complectation_name%3DStyle');
    });
});
