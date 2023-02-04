import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import VinReportNextPurchaseDiscount from './VinReportNextPurchaseDiscount';

it('не должен рендериться, если нет useless_report_promocode в сторе', () => {
    const page = shallow(<VinReportNextPurchaseDiscount uselessReportPromocode={ undefined }/>);
    expect(page).toBeEmptyRender();
});
