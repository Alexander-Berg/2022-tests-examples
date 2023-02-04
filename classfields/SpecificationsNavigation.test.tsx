import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import SpecificationsNavigation from './SpecificationsNavigation';

const Context = createContextProvider(contextMock);

async function renderComponent(params: any) {
    const wrapper = await render(
        <Context>
            <SpecificationsNavigation
                params={ params }
            />
        </Context>
        ,
    );

    return wrapper;
}

it('отконвертит новые параметры в старые', async() => {
    const { container } = await renderComponent({
        category: 'cars',
        mark: 'bmw',
        model: '5er',
        body_type_group: [
            'SEDAN',
            'HATCHBACK',
            'HATCHBACK_3_DOORS',
            'HATCHBACK_5_DOORS',
            'LIFTBACK',
        ],
        transmission: 'MECHANICAL',
        specification: 'razmer-ves',
    });
    const gallery = container.querySelector('.SpecificationsNavigation__link') as any;

    // eslint-disable-next-line max-len
    expect(gallery?.href).toEqual(`http://localhost/link/catalog-listing/?category=cars&mark=bmw&model=5er&autoru_body_type=SEDAN&autoru_body_type=HATCHBACK&autoru_body_type=HATCHBACK_3_DOORS&autoru_body_type=HATCHBACK_5_DOORS&autoru_body_type=HATCHBACK_LIFTBACK&transmission_full=MECHANICAL`);

});

it('если не надо конвертить параметры то прокинет старые', async() => {
    const { container } = await renderComponent({
        category: 'cars',
        mark: 'bmw',
        model: '5er',
        specification: 'razmer-ves',
    });
    const gallery = container.querySelector('.SpecificationsNavigation__link') as any;

    expect(gallery?.href).toEqual(`http://localhost/link/catalog-listing/?category=cars&mark=bmw&model=5er`);

});
