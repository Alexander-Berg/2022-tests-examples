import React from 'react';
import noop from 'lodash/noop';

import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';
import { ISalesDepartment } from 'realty-core/types/common';

import { PhoneViews } from '../types';
import { CardPhone } from '../';

const phoneViewsTestCases: [PhoneViews][] = [['dark'], ['soft-blue'], ['transparent-dark'], ['transparent-white']];

describe('CardPhone', () => {
    it.each(phoneViewsTestCases)('Верно рисует view %s', async (view) => {
        await render(
            <CardPhone
                geo={{} as IGeoStore}
                view={view}
                getSalesDepartment={noop}
                phone="+79991234567"
                salesDepartment={{} as ISalesDepartment}
                withBilling
            />,
            {
                viewport: { width: 300, height: 100 },
            }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
