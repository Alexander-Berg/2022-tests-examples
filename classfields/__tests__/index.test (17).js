import { shallow } from 'enzyme';
import Link from 'vertis-react/components/Link';
import i18n from 'realty-core/view/react/libs/i18n';
import { SalesDashboardWidgetComponent } from '../';

describe('SalesDashboardWidget', () => {
    beforeAll(() => {
        i18n.setLang('ru');
    });

    it('lets retry data fetch on error', () => {
        const onRetry = jest.fn();

        const wrapper = shallow(
            <SalesDashboardWidgetComponent
                callsData={{
                    status: 'errored',
                    callsStats: {}
                }}
                hideCallPrice={false}
                callPrice={0}
                onRetry={onRetry}
            />
        );

        const link = wrapper.find(Link);

        expect(link.exists()).toBe(true);
        expect(link.text()).toBe('Попробуйте снова');

        link.simulate('click');

        expect(onRetry).toBeCalled();
    });
});
