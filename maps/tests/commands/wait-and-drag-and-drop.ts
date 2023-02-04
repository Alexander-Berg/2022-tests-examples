import {wrapAsyncCommand} from '../lib/commands-utils';
import isMobileBrowser from '../lib/func/is-mobile-browser';

interface DragAndDropOptions {
    // <DraggableView> перестраивает DOM на лету, поэтому команда dragAndDrop не знает, куда перетащить элемент.
    // На десктопе можно использовать тачевый друг.
    imitateTouch?: boolean;
}

async function waitAndDragAndDrop(
    this: WebdriverIO.Browser,
    firstSelector: string,
    secondSelector: string,
    options?: DragAndDropOptions
): Promise<void> {
    const firstElement = await this.$(firstSelector);
    const secondElement = await this.$(secondSelector);
    await firstElement.waitForDisplayed();
    await secondElement.waitForDisplayed();

    if (!isMobileBrowser(this) && !options?.imitateTouch) {
        await firstElement.dragAndDrop(secondElement);
        return;
    }

    await this.performActions([
        {
            type: 'pointer',
            id: 'drag-and-drop',
            parameters: {pointerType: 'touch'},
            actions: [
                // hack: почему-то не происходит драг без этого действия.
                {
                    type: 'pointerMove',
                    origin: secondElement,
                    x: 0,
                    y: 0
                },

                {type: 'pointerDown', button: 0},
                {
                    type: 'pointerMove',
                    origin: firstElement,
                    x: 0,
                    y: 0
                },
                {type: 'pause', duration: 10},
                {
                    type: 'pointerMove',
                    duration: 500,
                    origin: secondElement,
                    x: 0,
                    y: 0
                },
                {type: 'pointerUp', button: 0}
            ]
        }
    ]);
    await this.releaseActions();
}

export default wrapAsyncCommand(waitAndDragAndDrop);
