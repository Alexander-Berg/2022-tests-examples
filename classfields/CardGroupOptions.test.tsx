import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import getAllCatalogComplectations from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/selectors/getAllCatalogComplectations';
import catalogSubtreeMock from 'auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree';
import cardGroupComplectationsMock from 'auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations';

import CardGroupOptions from './CardGroupOptions';

const complectations = getAllCatalogComplectations({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

it('CardGroupOptions должен отправить метрику view', () => {
    shallow(
        <CardGroupOptions
            catalogComplectations={ [] }
            complectationId={ null }
            complectations={ complectations }
            equipmentDictionary={{}}
            onChangeComplectation={ jest.fn() }
        />
        , { context: contextMock },
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'about_model', 'options', 'view' ]);
});
