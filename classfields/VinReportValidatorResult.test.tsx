/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import validationResult from 'www-cabinet/react/components/VinReportValidator/mocks/validationResultDataMock';

import VinReportValidatorResult from './VinReportValidatorResult';
import type { Tab } from './VinReportValidatorResult';

const defaultProps = {
    validationResult: validationResult,
    currentTab: 'errors' as Tab,
    onTabChange: () => {},
    onStatsClick: () => {},
    isChosen: false,
};

it('покажет ошибку для фида с неизвестным форматом', async() => {
    const validationResultCloned = _.cloneDeep(validationResult);
    validationResultCloned.feedFormat = 'azaza';
    const tree = shallowRenderComponent({ ...defaultProps, validationResult: validationResultCloned });

    expect(tree.find('.VinReportValidatorResult__format').text()).toEqual('Не удалось определить формат');
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <VinReportValidatorResult { ...props }/>,
    );
}
