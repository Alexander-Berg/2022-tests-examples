function wait(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function waitFor(
    predicate: () => Promise<boolean>,
    waitDescription: string,
    timeout = 3000,
    interval = 100
): Promise<void> {
    const maxTimestamp = Date.now() + timeout;
    while (Date.now() < maxTimestamp) {
        if (await predicate()) {
            return;
        }
        await wait(interval);
    }
    throw new Error(`${waitDescription} has timed out after ${timeout}ms`);
}

export async function waitForExpected(predicate: () => Promise<void>, waitDescription: string): Promise<void> {
    await waitFor(async () => {
        try {
            await predicate();
            return true;
        } catch (e) {
            return false;
        }
    }, waitDescription);
}
