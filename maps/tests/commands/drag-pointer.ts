import {wrapAsyncCommand} from '../lib/commands-utils';
import isMobileBrowser from '../lib/func/is-mobile-browser';

type FromParams = {selector: string} | {startPosition: Point};
type ToParams =
    | {endPosition: Point}
    | {delta: number}
    | {deltaX: number}
    | {deltaY: number}
    | {deltaX: number; deltaY: number};

// Скорость измеряется в пикселях в секунду
type MovementParams = {speed: number} | {duration: number} | {};
type CommonDragParams = MovementParams & {
    timeout?: number;
    description: string;
    forceTouch?: boolean;
    middleButton?: boolean;
    rightButton?: boolean;
    useMouseEvents?: boolean;
};
type Params = FromParams & ToParams & CommonDragParams;

type DragType = 'pointer' | 'touch';
type EventType = 'start' | 'move' | 'end';

async function dragPointer(this: WebdriverIO.Browser, params: Params): Promise<void> {
    if ('selector' in params) {
        await this.waitForVisible(params.selector, params.timeout);
    }
    const isMobile = isMobileBrowser(this);
    const maybeError = await this.executeAsync(
        executeDrag,
        isMobile || params.forceTouch ? 'touch' : 'pointer',
        await getParams(this, params)
    );
    if (maybeError) {
        throw new Error(`Error on drag execution: ${String(maybeError)}`);
    }
}

interface DragParams {
    duration: number;
    delta: Point;
    startPosition: Point;
    endPosition: Point;
    middleButton?: boolean;
    rightButton?: boolean;
    useMouseEvents?: boolean;
}

// px/s
const DEFAULT_DRAG_SPEED = 300;
const MINIMAL_DURATION = 10;
const MAXIMUM_DURATION = 10000;

async function getParams(browser: WebdriverIO.Browser, params: Params): Promise<DragParams> {
    const [startX, startY] = await getStartPosition(browser, params);
    const [endX, endY] = getEndPosition([startX, startY], params);
    const deltaX = endX - startX;
    const deltaY = endY - startY;
    const distance = Math.sqrt(Math.abs(deltaX) * Math.abs(deltaX) + Math.abs(deltaY) * Math.abs(deltaY));
    const durationBySpeed = Math.min(
        distance / (('speed' in params ? params.speed : DEFAULT_DRAG_SPEED) / 1000),
        MAXIMUM_DURATION
    );
    return {
        startPosition: [startX, startY],
        endPosition: [endX, endY],
        delta: [deltaX, deltaY],
        duration:
            'duration' in params && params.duration
                ? params.duration
                : distance === 0
                ? MINIMAL_DURATION
                : durationBySpeed,
        middleButton: params.middleButton,
        rightButton: params.rightButton,
        useMouseEvents: params.useMouseEvents
    };
}

async function getStartPosition(browser: WebdriverIO.Browser, params: FromParams): Promise<Point> {
    if ('startPosition' in params) {
        return params.startPosition;
    }
    return browser.getElementCenter(params.selector);
}

function getEndPosition([startX, startY]: Point, params: ToParams): Point {
    if ('endPosition' in params) {
        return params.endPosition;
    }
    const deltaX = 'delta' in params ? params.delta : 'deltaX' in params ? params.deltaX : 0;
    const deltaY = 'delta' in params ? params.delta : 'deltaY' in params ? params.deltaY : 0;
    return [startX + deltaX, startY + deltaY];
}

/* eslint-disable no-undef, prefer-arrow-callback, no-var */
function executeDrag(type: DragType, params: DragParams, done: (error?: string) => void) {
    function getPointerType(eventType: EventType) {
        switch (eventType) {
            case 'start':
                return 'pointerdown';
            case 'move':
                return 'pointermove';
            case 'end':
                return 'pointerup';
        }
    }
    function getMouseType(eventType: EventType) {
        switch (eventType) {
            case 'start':
                return 'mousedown';
            case 'move':
                return 'mousemove';
            case 'end':
                return 'mouseup';
        }
    }
    function getTouchType(eventType: EventType) {
        switch (eventType) {
            case 'start':
                return 'touchstart';
            case 'move':
                return 'touchmove';
            case 'end':
                return 'touchend';
        }
    }
    function getEvent(eventType: EventType, coordinates: Point): Event {
        if (type === 'pointer') {
            // https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/MouseEvent#values
            const button = params.rightButton ? 2 : params.middleButton ? 1 : 0;
            const buttons = params.rightButton ? 2 : params.middleButton ? 4 : 1;
            const eventName = params.useMouseEvents ? getMouseType(eventType) : getPointerType(eventType);

            return new window.PointerEvent(eventName, {
                button,
                buttons,
                bubbles: true,
                cancelable: true,
                clientX: coordinates[0],
                clientY: coordinates[1]
            });
        }
        const touch = new window.Touch({
            identifier: Date.now(),
            target: window.document.body,
            clientX: coordinates[0],
            clientY: coordinates[1],
            pageX: coordinates[0],
            pageY: coordinates[1]
        });

        return new window.TouchEvent(getTouchType(eventType), {
            // Для события touchend надо передавать пустой массив touches.
            // В противном случае браузер будет считать, что тач не окончен.
            touches: eventType === 'end' ? [] : [touch],
            targetTouches: [],
            changedTouches: [touch],
            bubbles: true,
            cancelable: true
        });
    }

    // Шаг меньше чем в 4px будет проигнорировать компонентом MobileSwipeable.
    var minPixelsInStep = type === 'touch' ? 4 : 2;
    var [startX, startY] = params.startPosition;
    var [deltaX, deltaY] = params.delta;
    var [x, y] = [startX, startY];
    var element: Element = window.document.elementFromPoint(x, y)!;
    if (!element) {
        throw new Error(`Can't get element on position [${x},${y}]`);
    }

    element.dispatchEvent(getEvent('start', params.startPosition));

    var startTimestamp = performance.now();
    function run(time: number) {
        try {
            var timePercentage = (time - startTimestamp) / params.duration;
            if (timePercentage >= 1) {
                element.dispatchEvent(getEvent('move', params.endPosition));
                element.dispatchEvent(getEvent('end', params.endPosition));
                done();
            } else {
                var nextX = startX + timePercentage * deltaX;
                var nextY = startY + timePercentage * deltaY;
                if (Math.abs(nextX - x) > minPixelsInStep || Math.abs(nextY - y) > minPixelsInStep) {
                    element = window.document.elementFromPoint(x, y) || element;
                    element.dispatchEvent(getEvent('move', [nextX, nextY]));
                    x = nextX;
                    y = nextY;
                }
                requestAnimationFrame(run);
            }
        } catch (e) {
            done(e.toString());
        }
    }
    requestAnimationFrame(run);
}
/* eslint-enable no-undef, prefer-arrow-callback, no-var */

export default wrapAsyncCommand(dragPointer);
export {FromParams, ToParams, CommonDragParams, Params, MovementParams};
