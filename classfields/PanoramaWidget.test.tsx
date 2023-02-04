jest.mock('../../../dataDomain/offers/actions/fetchOffer');
jest.mock('auto-core/react/lib/onDomContentLoaded');

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import onDomContentLoaded from 'auto-core/react/lib/onDomContentLoaded';
import getIdHash from 'auto-core/react/lib/offer/getIdHash';

import type { AppState as AppStateBase } from '../../../reducers/AppState';
import fetchOffer from '../../../dataDomain/offers/actions/fetchOffer';

import PanoramaWidget from './PanoramaWidget';
import type { OwnProps, AbstractProps } from './PanoramaWidget';

const fetchOfferMock = fetchOffer as jest.MockedFunction<typeof fetchOffer>;
fetchOfferMock.mockReturnValue((() => () => { }));

const onDomContentLoadedMock = onDomContentLoaded as jest.MockedFunction<typeof onDomContentLoaded>;
let onDomContentLoadedCallbacks: Array<() => void> = [];
onDomContentLoadedMock.mockImplementation((callback: () => void) => {
    onDomContentLoadedCallbacks.push(callback);
});

type Props = AbstractProps & {
    store: any;
}

class ComponentMock extends PanoramaWidget<Props, unknown> {
    renderPanoramaComponent() {
        return <div>panorama</div>;
    }

    handleWidgetClick = jest.fn()
}

type AppState = Pick<AppStateBase, 'offers'>;

const offerId = getIdHash(cardMock);
let props: OwnProps;
let initialState: AppState;

beforeEach(() => {
    onDomContentLoadedCallbacks = [];

    props = {
        options: {
            offerId,
            category: 'cars',
            type: 'exterior',
        },
    };

    initialState = {
        offers: {
            [offerId]: {
                data: cloneOfferWithHelpers(cardMock).withPanoramaExterior().value(),
                isFetching: false,
            },
        },
    };

    jest.useFakeTimers();
});

describe('условия показа', () => {
    it('нарисует компонент, если это внешняя панорама и она есть у оффера', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(false);
    });

    it('нарисует компонент, если это внутренняя панорама и она есть у оффера', () => {
        props.options.type = 'interior';
        initialState.offers[offerId].data = cloneOfferWithHelpers(cardMock).withPanoramaInterior().value();

        const { page } = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(false);
    });

    it('не нарисует компонент, если это внешняя панорама и ее нет у оффера', () => {
        initialState.offers[offerId].data = cloneOfferWithHelpers(cardMock).withPanoramaInterior().value();

        const { page } = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });

    it('не нарисует компонент, если это внутренняя панорама и ее нет у оффера', () => {
        props.options.type = 'interior';

        const { page } = shallowRenderComponent({ props, initialState });
        expect(page.isEmptyRender()).toBe(true);
    });
});

it('при загрузке страницы сделает запрос на получение оффера', () => {
    shallowRenderComponent({ props, initialState });
    onDomContentLoadedCallbacks.forEach((callback) => callback());

    expect(fetchOfferMock).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: AppState }) {
    const store = mockStore(initialState);

    const ConnectedComponentMock = ComponentMock.connector(ComponentMock);

    const page = shallow(
        <ConnectedComponentMock { ...props } store={ store }/>
        ,
        { context: contextMock },
    ).dive();

    return { page };
}
