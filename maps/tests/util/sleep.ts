/**
 * Simple Promise resolve after timeout. Could be used as "await sleep(100)" in async mocha tests.
 */
export function sleep(timeout: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, timeout));
};
