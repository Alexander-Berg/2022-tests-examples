function waitForVisible(
    this: WebdriverIO.Browser,
    selector: string,
    ms = this.options.waitforTimeout,
    reverse = false,
    inViewport = false
) {
    const isReversed = reverse ? '' : 'not ';
    const timeoutMsg = `element ("${selector}") still ${isReversed}visible after ${ms}ms`;

    return this.waitUntil(
        async () => {
            const elements = await this.$$(selector);

            if (elements.length === 0) {
                return reverse;
            }

            const elementsDisabled = await Promise.all(
                elements.map((element) => (inViewport ? element.isDisplayedInViewport() : element.isDisplayed()))
            );
            let result = reverse;

            for (const displayed of elementsDisabled) {
                if (!reverse) {
                    result = result || displayed;
                } else {
                    result = result && displayed;
                }
            }

            return result !== reverse;
        },
        ms,
        timeoutMsg
    );
}

export default waitForVisible;
