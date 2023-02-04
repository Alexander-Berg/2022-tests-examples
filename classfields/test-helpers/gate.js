import { GateError } from 'app/gates/libs/gate-error';
import rootGate from 'app/gates/root';
import { getUserPermissions } from 'app/libs/parse-user';
import { createBlackbox } from './blackbox';

function createGate(handle) {
    let pendingRequests = [];

    function decoratedHandle(name, params, opts) {
        const responseP = handle(name, params, opts)
            .catch(err => {
                if (GateError.isGateError(err)) {
                    throw err.data;
                }

                throw err;
            });

        if (! responseP.then) {
            throw 'Response is not a promise. Probably need to wrap mock response in Promise.resolve()';
        }

        const requestFinishP = responseP.catch(() => {});

        requestFinishP
            .then(() => {
                pendingRequests = pendingRequests.filter(req => req !== requestFinishP);
            });

        pendingRequests.push(requestFinishP);

        return responseP;
    }

    return {
        get: decoratedHandle,
        post: decoratedHandle,
        put: decoratedHandle,
        delete: decoratedHandle,
        waitForPendingRequests() {
            if (pendingRequests.length === 0) {
                return Promise.resolve();
            }

            return Promise
                .all(pendingRequests.map(req => req.catch(() => {})))
                .then(() => this.waitForPendingRequests());
        }
    };
}

function createGateFromBackend(backend, options) {
    return createGate((handleName, params) => {
        function callResource(resourceName, opts) {
            const resource = backend.resources[resourceName];

            if (! resource) {
                console.error(`Resource ${resourceName} is not mocked`); // eslint-disable-line no-console
                return Promise.reject(`Resource ${resourceName} is not mocked`);
            }

            return resource(opts);
        }
        function auth() {
            return Promise.resolve(backend.user);
        }
        auth.checkCrc = () => true;

        const req = {
            query: params,
            body: params,
            callResource,
            blackbox: createBlackbox(options.blackbox),
            bunker: {
                getNode() {
                    return null;
                }
            },
            auth
        };

        const handle = rootGate.handles[handleName];

        if (! handle) {
            console.error(`Handle ${handleName} is not defined`); // eslint-disable-line no-console
            return Promise.reject(`Handle ${handleName} is not defined`);
        }

        return handle
            .auth(req)
            .catch(err => {
                if (! GateError.isGateError(err)) {
                    console.error(err); // eslint-disable-line no-console
                }
                throw err;
            })
            .then(user => handle.response(req, user));
    });
}

export function createGateFromContext(options) {
    if (options.backend) {
        return createGateFromBackend(options.backend, options);
    }

    return createGate((name, params, opts) => {
        const mockHandle = options.gate && options.gate.handles[name];

        if (! mockHandle) {
            console.error(`Handle ${name} is not mocked`); // eslint-disable-line no-console
            return Promise.reject(`Handle ${name} is not mocked`);
        }

        return mockHandle(params, opts);
    });
}

export function createGateMock(handles) {
    return {
        handles,
        extend(additionalHandles) {
            return createGateMock({ ...handles, ...additionalHandles });
        }
    };
}

const defaultUser = {
    name: 'Locky',
    uid: '123',
    permissions: getUserPermissions('admin'),
    role: 'admin'
};

export function createBackendMock(resources, user = defaultUser) {
    return {
        resources,
        user,
        extend(additionalResources, newUser = user) {
            return createBackendMock({ ...resources, ...additionalResources }, newUser);
        }
    };
}
