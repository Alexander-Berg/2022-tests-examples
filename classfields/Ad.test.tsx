import React from 'react';
import { shallow } from 'enzyme';
import { AdView } from '@vertis/ads/build/client';

import { mockStore } from 'core/mocks/store.mock';
import { ADS_SERVICE_MOCK_1 } from 'core/services/ads/mocks/adsService.mock';
import { CONFIG_SERVICE_MOCK_1 } from 'core/services/config/mocks/configService.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ErrorBoundary } from 'core/client/components/ErrorBoundary/ErrorBoundary';

import { Ad, AdPlace } from './Ad';

beforeEach(() => {
    mockStore({
        [ServiceId.ADS]: ADS_SERVICE_MOCK_1,
        [ServiceId.CONFIG]: CONFIG_SERVICE_MOCK_1,
    });
});

it('реклама обёрнута в Error Boundary', () => {
    const wrapper = shallow(<WrapperComponent/>).dive();

    expect(wrapper.find(ErrorBoundary).exists()).toBe(true);
});

it('компоненту рекламы передаются все требуемые пропсы', () => {
    const wrapper = shallow(<WrapperComponent/>).dive().dive();
    const component = wrapper.find(AdView);

    expect(component.prop('ads')).toEqual(ADS_SERVICE_MOCK_1.data?.config);
    expect(component.prop('nonce')).toEqual(CONFIG_SERVICE_MOCK_1.data?.nonce);
    expect(component.prop('place')).toBe(AdPlace.DESKTOP_IN_ARTICLE);
    expect(component.prop('onError')).toBeInstanceOf(Function);
    expect(component.prop('uniq')).toBe('4321');
    expect(component.prop('rtb')).toEqual({
        pageNumber: 1,
    });
});

function WrapperComponent() {
    return (
        <Ad
            place={ AdPlace.DESKTOP_IN_ARTICLE }
            onError={ () => {} }
            rtb={{
                pageNumber: 1,
            }}
            uniq="4321"
        />
    );
}
