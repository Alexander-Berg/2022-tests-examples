import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';

import contextMock from 'autoru-frontend/mocks/contextMock';
import routerMock from 'autoru-frontend/mocks/routerMock';

import UniversalLink from 'auto-core/react/components/common/UniversalLink/UniversalLink';

import WalkInNoDataPlaceholder from './WalkInNoDataPlaceholder';

afterEach(() => {
    MockDate.reset();
});

const context = { ...contextMock, router: routerMock };

it('должен формировать правильные ссылки с мерджем параметров', () => {
    MockDate.set('2015-01-12');

    const tree = shallow(
        <WalkInNoDataPlaceholder
            routeParams={{ from: '2015-01-12', to: '2015-01-12', page: 1 }}
        />,
        { context },
    );

    const links = tree.find(UniversalLink);

    expect(shallowToJson(links)).toMatchSnapshot();
});
