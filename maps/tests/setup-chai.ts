import nock from 'nock';

const TIMEOUT = 100;

declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    export namespace Chai {
        interface Assertion {
            requested: Promise<void>;
        }
    }
}

function setupChai(chai: Chai.ChaiStatic): void {
    const {Assertion} = chai;

    Assertion.addProperty('requested', function (): Promise<void> {
        const url = this._obj.interceptors[0]._key;
        const assert = (isValid: boolean) => {
            this.assert(
                isValid,
                `expected Nock ${url} to have been requested`,
                `expected Nock ${url} to have not been requested`,
                isValid
            );
        };
        let timeout: NodeJS.Timeout;

        const promisifiedNock = (nockedRequest: nock.Scope): Promise<void> =>
            new Promise((resolve, reject) => {
                if (nockedRequest.isDone()) {
                    resolve();
                    return;
                }
                nockedRequest.once('replied', () => {
                    clearTimeout(timeout);
                    resolve();
                });
                nockedRequest.once('error', (message) => {
                    clearTimeout(timeout);
                    throw new Error(message);
                });
                timeout = setTimeout(() => {
                    reject();
                }, TIMEOUT);
            });

        return promisifiedNock(this._obj).then(
            () => assert(true),
            () => assert(false)
        );
    });
}

export default setupChai;
