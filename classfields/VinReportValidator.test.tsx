jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve('validationResult')),
    };
});

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import _ from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import gateApi from 'auto-core/react/lib/gateApi';

import VinReportValidator from './VinReportValidator';
import validationResult from './mocks/validationResult';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const store = {
    bunker: getBunkerMock([ 'cabinet/vin_report_validator' ]),
    vinReportValidator: {
        signedMdsUrl: {
            uploadUrl: 'uploaderUrl',
        },
    },
};

beforeEach(() => {
    getResource.mockClear();
});

it('должен вернуть VinReportValidator', () => {
    const vinReportValidatorInstance = shallow(
        <Provider store={ mockStore(store) }>
            <VinReportValidator/>
        </Provider>,
    ).dive().dive().instance();

    vinReportValidatorInstance.setState({ validationResult });

    expect(vinReportValidatorInstance.render()).toMatchSnapshot();
});

describe('onSubmit', () => {
    it('должен вызвать gateApi и установить корректный state', () => {
        getResource.mockImplementation(() => Promise.resolve({}));

        const vinReportValidatorInstance: any = shallow(
            <Provider store={ mockStore(store) }>
                <VinReportValidator/>
            </Provider>,
        ).dive().dive().instance();

        vinReportValidatorInstance.setState = jest.fn();

        return (vinReportValidatorInstance.onSubmit())
            .then(() => {
                expect(vinReportValidatorInstance.setState.mock.calls).toEqual([
                    [ { isValidating: true } ],
                    [ {
                        isValidating: false,
                        validationResult: {
                            errorCode: 'REQUEST_ERROR',
                            general: {
                                countFile: 0,
                                feedValidation: [],
                            },
                        },
                    } ],
                ]);
            });
    });

    it('должен вызвать gateApi и установить корректный state, если произошла ошибка', () => {
        getResource.mockImplementation(() => Promise.reject('validationResult'));

        const vinReportValidatorInstance: any = shallow(
            <Provider store={ mockStore(store) }>
                <VinReportValidator/>
            </Provider>,
        ).dive().dive().instance();

        vinReportValidatorInstance.setState = jest.fn();
        return (vinReportValidatorInstance.onSubmit())
            .then(() => {
                expect(vinReportValidatorInstance.setState.mock.calls).toEqual([
                    [ { isValidating: true } ],
                    [ {
                        isValidating: false,
                        validationResult: {
                            errorCode: 'REQUEST_ERROR',
                            general: {
                                countFile: 0,
                                feedValidation: [],
                            },
                        },
                    } ],
                ]);
            });
    });
});

describe('onFileUploadingChange', () => {
    it('должен вызвать xhr.open и xhr.send', () => {
        const open = jest.fn();
        const send = jest.fn();
        const append = jest.fn();
        const xhrMockClass: any = jest.fn().mockImplementation(() => ({
            open,
            send,
            response: 'fileUrl',
            readyState: 4,
        }));
        global.XMLHttpRequest = xhrMockClass;
        global.FormData = jest.fn().mockImplementation(() => ({
            append,
        }));

        const vinReportValidatorInstance: any = shallow(
            <Provider store={ mockStore(store) }>
                <VinReportValidator/>
            </Provider>,
        ).dive().dive().instance();

        vinReportValidatorInstance.setState = jest.fn();
        vinReportValidatorInstance.onFileUploadingChange({
            target: {
                files: [ { size: 2, name: 'car.csv' } ],
            },
            preventDefault: _.noop,
            stopPropagation: _.noop,
        });

        expect(append).toHaveBeenCalledWith('file', { size: 2, name: 'car.csv' });
        expect(open).toHaveBeenCalledWith('POST', 'uploaderUrl&name=car.csv', true);
        expect(send).toHaveBeenCalledWith({ append });
    });
});
