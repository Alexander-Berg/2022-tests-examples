import {wrapAsyncCommand} from '../../tests/lib/commands-utils';

async function addStyles(this: WebdriverIO.Browser, styles: string): Promise<() => Promise<void>> {
    /* eslint-disable prefer-arrow-callback,no-undef,no-var */
    const styleElement = await this.execute(function (styles): HTMLStyleElement {
        var styleElement = window.document.createElement('style');
        styleElement.innerHTML = styles;
        window.document.head.appendChild(styleElement);
        return styleElement;
    }, styles);
    return async () => {
        await this.execute(function (styleElement): void {
            window.document.head.removeChild(styleElement);
        }, styleElement);
    };
}

export default wrapAsyncCommand(addStyles);
