interface WaitForHiddenOptions {
    timeout?: number;
    inViewport?: boolean;
}

function waitForHidden(this: WebdriverIO.Browser, selector: string, options: WaitForHiddenOptions = {}) {
    return this.waitForVisible(selector, options.timeout, true, options.inViewport);
}

export default waitForHidden;
