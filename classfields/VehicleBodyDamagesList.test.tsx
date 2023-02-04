import React from 'react';
import _ from 'lodash';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';

import VehicleBodyDamagesList, { DOT_COLOR, DOT_SIZE } from './VehicleBodyDamagesList';

it('не должен ничего рендерить, если нет повреждений', () => {
    render(
        <VehicleBodyDamagesList
            damages={ [] }
            onItemMouseEnter={ _.noop }
            onItemMouseLeave={ _.noop }
            dotSize={ DOT_SIZE.M }
            dotColor={ DOT_COLOR.GRAY }
            outerActiveIndex={ null }
        />,
    );

    const container = document.querySelector('.VehicleBodyDamagesList');

    expect(container).not.toBeInTheDocument();
});
