import {wrapAsyncCommand} from '../../tests/lib/commands-utils';

async function addClass(this: WebdriverIO.Browser, selector: string, className: string): Promise<() => Promise<void>> {
    /* eslint-disable prefer-arrow-callback,no-undef,no-var */
    const elements: Element[] = await this.execute(
        function (selector: string, className: string): Element[] {
            return Array.from(window.document.querySelectorAll(selector)).map((element) => {
                element.classList.add(className);
                return element;
            });
        },
        selector,
        className
    );
    return async () => {
        await this.execute(
            function (elements: Element[], className: string): void {
                elements.forEach((element) => {
                    element.classList.remove(className);
                });
            },
            elements,
            className
        );
    };
}

export default wrapAsyncCommand(addClass);
