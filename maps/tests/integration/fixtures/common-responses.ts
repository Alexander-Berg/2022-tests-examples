export const blackboxLoginValidResponse = {
    users: [{
        error: 'OK',
        status: {
            value: 'VALID',
            uid: '9999'
        },
        login: 'bankwerker'
    }]
};

export const blackboxUidValidResponse = {users: [{
    error: 'OK',
    status: {
        value: 'VALID',
        login: 'sample_login'
    },
    uid: {
        value: '1234'
    }
}]};

export const blackboxSessionidResponse = {
    error: 'OK',
    status: {
        value: 'VALID'
    },
    uid: {
        value: '1234'
    },
    login: 'test-login'
};

export const tvmResponse = {
    blackbox: {
        ticket: 'sample_ticket',
        tvm_id: 239
    }
};

export const tvmIntResponse = {
    blackboxInternal: {
        ticket: 'sample_int_ticket',
        tvm_id: 223
    }
};
