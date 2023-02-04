import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import FormFieldsSelector from 'auto-core/react/components/common/FormFieldsSelector/FormFieldsSelector';
import mockFormFieldsCarsEvaluation from 'auto-core/react/dataDomain/formFields/mocks/carsEvaluation';

import carsEvaluationTechOptions from './mocks/carsEvaluationTechOptions';

it('комплектации нет на форме после выбора tech_param, если опции еще грузятся', () => {
    const state = {
        formFields: mockFormFieldsCarsEvaluation,
        user: { data: {} },
    };

    const page = shallow(
        <FormFieldsSelector
            onChangeForm={ noop }
            params={{ category: 'cars' }}
            techOptions={ carsEvaluationTechOptions }
            type="add"
            depsOptions={{ techOptionsPending: true }}
        />,
        { context: { ...contextMock, store: mockStore(state) } },
    ).dive();
    const complectation = page.find('CarsComplectation');
    expect(complectation.isEmptyRender()).toBe(true);
});

it('комплектация есть на форме после выбора tech_param, если опции загрузились', () => {
    const state = {
        formFields: mockFormFieldsCarsEvaluation,
        user: { data: {} },
    };

    const page = shallow(
        <FormFieldsSelector
            onChangeForm={ noop }
            params={{ category: 'cars' }}
            techOptions={ carsEvaluationTechOptions }
            type="add"
            depsOptions={{ techOptionsPending: false }}
        />,
        { context: { ...contextMock, store: mockStore(state) } },
    ).dive();
    const complectation = page.find('CarsComplectation');
    expect(complectation.isEmptyRender()).toBe(false);
});
