function waitForExist(this: WebdriverIO.Browser, selector: string, ms = this.options.waitforTimeout, reverse = false) {
    const isReversed = reverse ? '' : 'not ';
    const timeoutMsg = `element ("${selector}") still ${isReversed}existing after ${ms}ms`;

    return this.waitUntil(
        async () => {
            const elements = await this.$$(selector);

            if (elements.length === 0) {
                return reverse;
            }

            const elementsExisting = await Promise.all(elements.map((element) => element.isExisting()));
            let result = reverse;

            for (const exist of elementsExisting) {
                if (!reverse) {
                    result = result || exist;
                } else {
                    result = result && exist;
                }
            }

            return result !== reverse;
        },
        ms,
        timeoutMsg
    );
}

export default waitForExist;
