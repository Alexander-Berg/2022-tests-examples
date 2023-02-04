import type { CatalogTechParamAction } from 'auto-core/react/dataDomain/catalogTechParam/types';
import { PAGE_LOADING_SUCCESS } from 'auto-core/react/actionTypes';
import catalogTechParamMock from 'auto-core/react/dataDomain/catalogTechParam/mocks/catalogTechParam.mock';

import reducer from './reducer';

it('должен положить данные при загрузке страницы', () => {
    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            catalogTechParam: catalogTechParamMock.data,
        },
    } as CatalogTechParamAction;

    const result = reducer(undefined, action);

    expect(result.data).toEqual(catalogTechParamMock.data);
});
