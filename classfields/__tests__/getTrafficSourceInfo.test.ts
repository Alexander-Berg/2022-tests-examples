import analiticsData from 'realty-core/view/react/libs/analiticsData';

import { ANALYTIC_DATA } from '../../__tests__/mocks';

import { getTrafficSourceInfo } from '../getTrafficSourceInfo';

jest.mock('realty-core/view/react/libs/analiticsData', () => ({
    get: jest.fn(),
}));

describe('getTrafficSourceInfo', () => {
    it('Возвращает ничего', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ({}));

        expect(getTrafficSourceInfo()).toMatchSnapshot();
    });

    it('Возвращает заполненный объект', () => {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        analiticsData.get.mockImplementation(() => ANALYTIC_DATA);

        expect(getTrafficSourceInfo()).toMatchSnapshot();
    });
});
