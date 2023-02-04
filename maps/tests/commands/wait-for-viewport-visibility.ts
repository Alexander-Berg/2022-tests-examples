import {wrapAsyncCommand} from '../lib/commands-utils';

type VisibilityOption = 'fullyVisible' | 'fullyInvisible' | 'atLeastPartiallyVisible';

async function waitForViewportVisibility(
    this: WebdriverIO.Browser,
    selector: string,
    visibilityOption: VisibilityOption,
    timeout?: number
): Promise<void> {
    let lastVisibilityOptions: boolean[] = [];
    try {
        await this.waitUntil(
            async () => {
                const {top, bottom, left, right} = await this.getViewportVisibility(selector);
                lastVisibilityOptions = [top, right, bottom, left];
                switch (visibilityOption) {
                    case 'fullyVisible':
                        return left && right && top && bottom;
                    case 'fullyInvisible':
                        // Элемент не видно если не видно ни верха ни низа (он выше / ниже экрана)
                        // либо если не видно ни лево ни право (он левее / правее экрана)
                        return (!left && !right) || (!top && !bottom);
                    case 'atLeastPartiallyVisible':
                        return left || right || top || bottom;
                }
            },
            timeout,
            'TIMEOUT'
        );
    } catch (e) {
        if (e.message.includes(`TIMEOUT`)) {
            throw new Error(`Элемент ${selector}: ` + getExpectedText(visibilityOption, lastVisibilityOptions));
        }
        throw e;
    }
}

const SIDES = ['верх', 'право', 'низ', 'лево'];

function getExpectedText(option: VisibilityOption, visibleSides?: boolean[]): string {
    let visibleText = '';
    if (visibleSides && visibleSides.length !== 0) {
        const visibleSidesText = visibleSides
            .map((side, index) => (side ? SIDES[index] : undefined))
            .filter(Boolean)
            .join(', ');
        visibleText =
            visibleSidesText.length === 0 ? '\nНи одна сторона не видна.' : `\nВидны стороны: ${visibleSidesText}.`;
    }
    switch (option) {
        case 'fullyVisible':
            return 'ожидается что все стороны будут видны' + visibleText;
        case 'fullyInvisible':
            return 'ожидается что все стороны будут не видны' + visibleText;
        case 'atLeastPartiallyVisible':
            return 'ожидается что хотя бы частично элемент будет виден' + visibleText;
    }
}

export default wrapAsyncCommand(waitForViewportVisibility);
export {getExpectedText};
