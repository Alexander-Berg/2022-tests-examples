/* eslint-disable @typescript-eslint/no-explicit-any */
import React, {createRef, useCallback, useEffect, useState} from 'react';
import ReactDom from 'react-dom';
import {act} from 'react-dom/test-utils';
import reactify from '../../../react/index';
import internalReactify from '../../../react/reactify';
import {requestMeta} from '../../../imperative/utils';
import {LngLat, YMapControlButton as YMapControlButtonI, YMapControls as YMapControlsI} from '../../../imperative';
import type {YMapConfig, YMapLocation} from '../../../imperative/YMap';
import {traverseInit, TraverseOrder, TraverseState, traverseStep} from '../../../imperative/traverse';
import type {ComplexEntity} from '../../../imperative/Entities';
import {YMapZoomControl as YMapZoomControlI, YMapGeolocationControl as YMapGeolocationControlI} from '../index';

const {YMap, YMapControls, YMapReactContainer, YMapControl, YMapControlButton} = reactify(React, ReactDom);

const {reactifyEntity} = internalReactify(React, ReactDom);
declare function domToJson(e: Node | ChildNode | null): object;

const YMapZoomControl = reactifyEntity(YMapZoomControlI);
const YMapGeolocationControl = reactifyEntity(YMapGeolocationControlI);

function run(traverse: TraverseState) {
    const result: string[] = [];
    // eslint-disable-next-line no-constant-condition
    while (true) {
        traverseStep(traverse);
        if (!traverse.current) {
            break;
        }
        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
        const impl = traverse.current as unknown as {_props: any};
        result.push(impl.constructor.name + (impl._props.text ? `:${impl._props.text}` : ''));
    }
    return result;
}

const YMapControlButtonCounterR = reactifyEntity(
    class YMapControlButtonCounter extends YMapControlButtonI {
        _timer?: number;
        _onAttach() {
            this._timer = window.setInterval(() => this.update({text: String(Number(this.text) + 1)}), 500);
        }
        _onDetach() {
            window.clearInterval(this._timer);
        }
    }
);

const ReactControl = () => {
    const [n, setN] = useState(0);

    useEffect(() => {
        const id = setInterval(() => setN((n) => n + 1), 500);
        return () => clearInterval(id);
    }, []);

    return (
        <YMapControl>
            <YMapReactContainer tagName="div">
                <button
                    style={{display: 'block', background: 'white', borderRadius: '8px', padding: '8px', border: '0'}}
                >
                    react!&nbsp;{n}
                </button>
            </YMapReactContainer>
        </YMapControl>
    );
};

const FooBarQuxControlGroup = () => (
    <>
        <YMapControlButton text="#foo" />
        <YMapControlButton text="#bar" />
        <YMapControlButton text="#qux" />
    </>
);

const Gog999ControlGroup = () => (
    <>
        <YMapControlButton text="@god" />
        <YMapControlButton text="@999" />
    </>
);

export const MixControls = ({location, config}: {location: YMapLocation; config: YMapConfig}) => {
    const controlsRef = createRef<YMapControlsI>();

    Object.assign(window, {controlsRef});

    const [buttons, setButtons] = useState(() => [
        <YMapControlButton key="top" text="top" />,
        <FooBarQuxControlGroup key="FooBarQux" />,
        <ReactControl key="react" />,
        <YMapControlButton key="kek" text="kek" />,
        <Gog999ControlGroup key="God999" />,
        <YMapControlButtonCounterR key="counter" text="0" />
    ]);

    const shiftLeft = useCallback(() => {
        setButtons((buttons) => [...buttons.slice(1), buttons[0]]);
    }, []);

    const shiftRight = useCallback(() => {
        setButtons((buttons) => [...buttons.slice(-1), ...buttons.slice(0, -1)]);
    }, []);

    return (
        <YMap location={location} config={config} mode="raster">
            <YMapControls position="top left horizontal" ref={controlsRef}>
                <YMapControlButton text="<" onClick={shiftLeft} />
                {buttons}
                <YMapControlButton text=">" onClick={shiftRight} />
                <YMapZoomControl />
                <YMapGeolocationControl />
            </YMapControls>
        </YMap>
    );
};

describe('Order controls', () => {
    const LOCATION = {center: [53, 37] as LngLat, zoom: 12};
    let config: YMapConfig;

    let container: HTMLElement;

    beforeAll(async () => {
        const meta = await requestMeta('ru_RU', {environment: 'testing'});
        config = {meta};
    });

    beforeEach(() => {
        jest.useFakeTimers();
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        jest.useRealTimers();
        container.remove();
    });

    describe('Check right react components order', () => {
        it('should render correct react order', () => {
            act(() => {
                ReactDom.render(<MixControls location={LOCATION} config={config} />, container);
            });

            act(() => {
                jest.advanceTimersByTime(1000);
            });

            expect(domToJson(container.firstChild)).toMatchSnapshot();

            act(() => {
                const btn = container.querySelector('button');
                btn?.dispatchEvent(new MouseEvent('click', {bubbles: true}));
            });

            act(() => {
                jest.advanceTimersByTime(1000);
            });

            expect(domToJson(container.firstChild)).toMatchSnapshot();
        });

        it('should render correct imperative order', () => {
            act(() => {
                ReactDom.render(<MixControls location={LOCATION} config={config} />, container);
            });

            act(() => {
                jest.advanceTimersByTime(1000);
            });

            let controlsRef: ComplexEntity<unknown> = (window as any).controlsRef.current;

            const orders = run(traverseInit(controlsRef, {recurse: () => true, order: TraverseOrder.PREORDER}));
            expect(orders).toMatchSnapshot();

            act(() => {
                const btn = container.querySelector('button');
                btn?.dispatchEvent(new MouseEvent('click', {bubbles: true}));
            });

            act(() => {
                jest.advanceTimersByTime(1000);
            });

            const orders2 = run(traverseInit(controlsRef, {recurse: () => true, order: TraverseOrder.PREORDER}));
            expect(orders2).toMatchSnapshot();
        });
    });
});
