import { shallow } from 'enzyme';
import Link from 'vertis-react/components/Link';
import i18n from 'realty-core/view/react/libs/i18n';
import { ViewsDashboardWidgetComponent } from '../';

const onRetry = jest.fn();

describe('ViewsDashboardWidget', () => {
    beforeAll(() => {
        i18n.setLang('ru');
    });

    afterEach(() => {
        onRetry.mockReset();
    });

    it('lets retry views fetch on error', () => {
        const wrapper = shallow(
            <ViewsDashboardWidgetComponent
                viewsData={{
                    status: 'errored'
                }}
                onRetry={onRetry}
                periodToShow={{}}
            />
        );

        const link = wrapper.find(Link);

        expect(link.text()).toBe('Попробовать снова');

        link.simulate('click');

        expect(onRetry).toBeCalled();
    });
});
