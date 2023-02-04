import React from 'react';
import { shallow } from 'enzyme';

import type { ModelCompareData } from '@vertis/schema-registry/ts-types-snake/auto/api/compare_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import versusMock from 'auto-core/react/dataDomain/versus/mock';

import VersusPriceCell from './VersusPriceCell';

const ContextProvider = createContextProvider(contextMock);

const versus = versusMock.value();

it('не должен падать и что-то рисовать, если не передана модель', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusPriceCell model={{} as ModelCompareData}/>
        </ContextProvider>,
    ).dive();

    expect(tree).toBeEmptyRender();
});

it('передаёт правильные параметры для ссылки на листинг', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusPriceCell model={ versus[0].model }/>
        </ContextProvider>,
    ).dive();

    const link = tree.find('Button').prop('url');

    expect(link).toEqual('link/listing/?category=cars&section=all' +
        '&mark=FORD&model=ECOSPORT&super_gen=20104320&configuration_id=21738487&tech_param_id=21738490');
});

it('если офферов нет - пишет соответствующее сообщение', () => {
    const tree = shallow(
        <ContextProvider>
            <VersusPriceCell model={ versus[1].model }/>
        </ContextProvider>,
    ).dive();

    const text = tree.find('.VersusPriceCell__title').text();

    expect(text).toEqual('Нет в продаже');
});
