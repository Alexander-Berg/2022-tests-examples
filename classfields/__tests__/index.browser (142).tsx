import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FiltersFormSelectedFilters } from '../index';

import { emptyFunctionMock, selectedFiltersDeclaration } from './mocks';

const renderOptions = { viewport: { width: 500, height: 250 } };

describe('FiltersFormSelectedFilters', () => {
    it('Базовое состояние', async () => {
        await render(
            <FiltersFormSelectedFilters
                selectedFiltersDeclaration={selectedFiltersDeclaration}
                onFiltersChange={emptyFunctionMock}
            />,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
