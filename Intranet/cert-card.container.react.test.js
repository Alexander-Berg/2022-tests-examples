/* eslint max-nested-callbacks: ["error", 5] */

import React from 'react';
import CertCardContainer from 'b:cert-card.container';
import {mount} from 'enzyme';
import {ESC, ENTER} from 'b:keycodes';
import inherit from 'inherit';
import clone from 'lodash/cloneDeep';
import wait from '@crt/wait';

describe('cert-card.container', () => {

    let data;
    let dataFull;
    let dataAbc;

    let filterMatchStr;
    let updateQueryStr;
    let query;
    let state;

    let context;

    beforeEach(() => {
        updateQueryStr = jest.fn();
        filterMatchStr = jest.fn();

        query = {param: ['value']};
        state = {param: ['value']};

        data = {
            certId: '11',
            expr: '/:certId',
            path: '/11',
            cert: {
                id: 11,
                type: 'host',
                additionalFieldsConfig: [
                    {slug: 'extended_validation'},
                    {slug: 'desired_ttl_days'},
                    {slug: 'hosts'},
                    {slug: 'abc_service'}
                ],
                available_actions: [
                    {
                        id: 'download',
                        name: {ru: 'Скачать'}
                    }
                ],
                serial_number: 'serial-number',
                common_name: 'common name',
                ca_name: 'ca-name',
                type_human: {ru: 'type'},
                status_human: {ru: 'status'},
                device_platform: 'platform',
                end_date: '2018-06-28T19:37:53+03:00',
                requester: {username: 'remnev'},
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}],
                extended_validation: true,
                desired_ttl_days: 2,
                hosts: ['local.local'],
                abc_service: {id: 3},
                added: '2018-06-28T19:37:53+03:00',
                issued: '2018-06-28T19:37:53+03:00',
                priv_key_deleted_at: '2018-06-28T19:37:53+03:00'
            },
            requestCert: jest.fn(),
            requestCertAction: jest.fn(),
            requestUpdateAbcService: jest.fn(),
            requestAdditionalFieldsConfig: jest.fn(),
            addBPageClickListener: jest.fn(),
            deleteBPageClickListener: jest.fn(),
            filterMatchStr,
            updateQueryStr
        };

        dataFull = {
            ...data,
            cert: {
                ...data.cert,
                id: 0,
                available_actions: [
                    {
                        id: 'revoke',
                        name: {ru: 'Отозвать'}
                    }
                ],
                used_template: 'template',
                tags: [{name: 'a', is_active: true}, {name: 'b', is_active: false}]
            }
        };

        dataAbc = {
            ...dataFull,
            cert: {
                ...dataFull.cert,
                abc_service: {id: 11}
            }
        };

        context = {
            addBPageClickListener: jest.fn(),
            deleteBPageClickListener: jest.fn()
        };
    });

    it('should start with loading', async () => {
        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {context}
        );

        expect(wrapper).toMatchSnapshot();

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should contain a proper content after load', async () => {
        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {context}
        );

        await wait()
            .then(() => {
                wrapper.update();
                expect(wrapper).toMatchSnapshot();
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    describe('cert reissueing', () => {
        it('should call a change URL method', async () => {
            let wrapper;

            await wait()
                .then(() => {
                    wrapper = mount(<CertCardContainer {...data} />, {context});
                })
                .then(() => {
                    wrapper.update();
                    wrapper.find('.cert-card__button_type_reissue').simulate('click');
                })
                .then(() => {
                    expect(updateQueryStr).toHaveBeenCalled();
                })
                .finally(() => {
                    wrapper.unmount();
                });
        });

        it('should properly handle several hosts (keeps them as a String, not Array)', async () => {
            const patchedData = {
                ...data,
                cert: {
                    ...data.cert,
                    hosts: [
                        ...data.cert.hosts,
                        'another.local'
                    ]
                }
            };

            let wrapper;

            await wait()
                .then(() => {
                    wrapper = mount(<CertCardContainer {...patchedData} />, {context});
                })
                .then(() => {
                    wrapper.update();
                    wrapper.find('.cert-card__button_type_reissue').simulate('click');
                })
                .then(() => {
                    expect(updateQueryStr).toHaveBeenCalledWith({
                        'cr-form': 1,
                        'cr-form-abc_service': 3,
                        'cr-form-ca_name': 'ca-name',
                        'cr-form-desired_ttl_days': 2,
                        'cr-form-extended_validation': true,
                        'cr-form-hosts': 'local.local,another.local', // <– String
                        'cr-form-type': 'host'
                    });
                })
                .finally(() => {
                    wrapper.unmount();
                });
        });
    });

    it('should set additionalFieldsConfigError when requestAdditionalFieldsConfig returns error', () => {
        const PatchedCertCardContainer = inherit(CertCardContainer, {
            async handleReissueButtonClick() {
                const base = ::this.__base;

                try {
                    await base(...arguments);
                } catch (e) {
                    // Noop
                }
            }
        });
        const requestAdditionalFieldsConfig = () => {
            throw new Error('some error');
        };
        const wrapperData = {
            ...data,
            requestAdditionalFieldsConfig
        };
        const wrapper = mount(
            <PatchedCertCardContainer
                {...wrapperData}
            />,
            {context}
        );

        return wait()
            .then(() => {
                wrapper.update();
                wrapper.find('.cert-card__button_type_reissue').simulate('click');

                expect(wrapper.state().additionalFieldsConfigError).toEqual('Error: some error');
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should refetch data when receives new cert id', async () => {
        const initialId = '11';
        const newId = '22';
        const requestCert = jest.fn();

        data.requestCert = requestCert;

        const wrapper = mount(
            <CertCardContainer {...data} />,
            {context}
        );

        expect(requestCert).toHaveBeenCalledWith(initialId);
        requestCert.mockReset();

        wrapper.setProps({certId: newId});
        expect(requestCert).toHaveBeenCalledWith(newId);

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should update an ABC-service field when receives new abc-service id', async () => {
        const newId = 22;
        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {
                context
            }
        );

        expect(wrapper.state('abcServiceId')).toStrictEqual(3);

        wrapper.setProps({
            cert: {
                ...data.cert,
                abc_service: {id: newId}
            }
        });
        expect(wrapper.state('abcServiceId')).toStrictEqual(newId);

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should be closed when close click happens', async () => {
        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {context}
        );

        await wait()
            .then(() => {
                wrapper.update();
                wrapper.find('.cert-card__button-close').simulate('click');

                expect(filterMatchStr.mock.calls.length).toEqual(1);

                wrapper.unmount();
            });
    });

    it('should not update query or state on close', (done) => {

        const prevQuert = clone(query);
        const prevState = clone(state);

        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {
                context
            }
        );

        setTimeout(() => {
            wrapper.update();
            wrapper.find('.cert-card__button-close').simulate('click');

            expect(filterMatchStr).toHaveBeenCalled();

            expect(prevState).toEqual(state);
            expect(prevQuert).toEqual(query);

            wrapper.unmount();
            done();
        }, 0);
    });

    it('should refetch data when retry click happens', () => {
        const requestCert = jest.fn();
        const certId = '11';

        data.requestCert = requestCert;

        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {context}
        );

        return wait()
            .then(() => {
                wrapper.update();
                wrapper.setState({fetchError: new Error('error')});

                requestCert.mockReset();
                wrapper.find('.cert-card__button-retry').simulate('click');

                expect(requestCert).toHaveBeenCalledWith(certId);
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should be closed when ESC was pressed', async () => {
        const wrapper = mount(
            <CertCardContainer {...data} />,
            {context}
        );
        const event = new window.KeyboardEvent('keydown', {keyCode: ESC});

        window.dispatchEvent(event);

        expect(filterMatchStr.mock.calls.length).toEqual(1);

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should not be closed when any key (except of ESC) was pressed', async () => {
        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {context}
        );
        const event = new window.KeyboardEvent('keydown', {keyCode: ENTER});

        window.dispatchEvent(event);
        expect(filterMatchStr).not.toHaveBeenCalled();

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should be closed when click on anything (except of the card itself) happens', () => {
        context.addBPageClickListener = (fn) => {
            const target = document.createElement('div');

            fn({target});
        };

        const wrapper = mount(
            <CertCardContainer
                {...data}
            />,
            {
                context
            }
        );

        return wait()
            .then(() => {
                expect(filterMatchStr.mock.calls.length).toEqual(1);
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should not be closed if click happened on an element inside the table', (done) => {

        context.addBPageClickListener = (fn) => {
            const target = {
                closest() {
                    return true;
                }
            };

            fn({target});
        };

        const wrapper = mount(
            <CertCardContainer {...data} />,
            {
                context
            }
        );

        setTimeout(() => {
            expect(filterMatchStr).not.toHaveBeenCalled();
            wrapper.unmount();

            done();
        }, 0);
    });

    it('should not be closed if click happened on an element inside the card', () => {
        const PatchedCertCardContainer = inherit(CertCardContainer, {
            getRef() {
                return () => ({
                    contains() {
                        return true;
                    }
                });
            }
        });

        context.addBPageClickListener = (fn) => {
            const target = document.createElement('div');

            fn({target});
        };

        const wrapper = mount(
            <PatchedCertCardContainer
                {...data}
            />,
            {context}
        );

        return wait()
            .then(() => {
                expect(filterMatchStr).not.toHaveBeenCalled();
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should do action request', () => {
        const requestCertAction = jest.fn();

        dataFull.requestCertAction = requestCertAction;

        const wrapper = mount(
            <CertCardContainer {...dataFull} />,
            {context}
        );

        return wait()
            .then(() => {
                wrapper.update();
                wrapper.find('.cert-card__actions .cert-card__button').at(0).simulate('click');
                expect(requestCertAction).toHaveBeenCalled();
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should handle an error of action request', () => {
        function requestCertAction() {
            throw new Error('error');
        }

        const PatchedCertCardContainer = inherit(CertCardContainer, {
            async handleActionClick() {
                try {
                    await this.__base(...arguments);
                } catch (e) {
                    // Noop
                }
            }
        });

        dataFull.requestCertAction = requestCertAction;

        const wrapper = mount(
            <PatchedCertCardContainer {...dataFull} />,
            {context}
        );

        return wait()
            .then(() => {
                wrapper.update();
                wrapper.find('.cert-card__actions .cert-card__button').at(0).simulate('click');
                expect(wrapper.state('actionError')).toEqual({
                    id: 'revoke',
                    text: 'Error: error'
                });
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    it('should fetch data immediately after it was mounted', async () => {
        const certId = '11';
        const requestCert = jest.fn();

        data.requestCert = requestCert;

        const wrapper = mount(
            <CertCardContainer {...data} />,
            {
                context
            }
        );

        expect(requestCert).toHaveBeenCalledWith(certId);
        requestCert.mockReset();

        await wait()
            .then(() => {
                wrapper.unmount();
            });
    });

    it('should handle an error of fetching data', () => {
        function requestCert() {
            throw new Error('error');
        }

        const PatchedCertCardContainer = inherit(CertCardContainer, {
            async componentDidMount() {
                try {
                    await this.__base(...arguments);
                } catch (e) {
                    // Noop
                }
            }
        });

        data.requestCert = requestCert;

        const wrapper = mount(
            <PatchedCertCardContainer {...data} />,
            {
                context
            }
        );

        expect(wrapper.state('fetchError')).toBeInstanceOf(Error);

        wrapper.unmount();
    });

    it('should hide an action-error tooltip after the click on anything happened', () => {
        function requestCertAction() {
            throw new Error('error');
        }

        const PatchedCertCardContainer = inherit(CertCardContainer, {
            async handleActionClick() {
                try {
                    await this.__base(...arguments);
                } catch (e) {
                    // Noop
                }
            }
        });

        dataFull.requestCertAction = requestCertAction;

        const wrapper = mount(
            <PatchedCertCardContainer {...dataFull} />,
            {context}
        );

        return wait()
            .then(() => {
                wrapper.update();

                // No tooltip
                expect(document.querySelectorAll('.cert-card__action-error-tooltip').length).toBe(0);

                // Tooltip exists
                wrapper.find('.cert-card__actions .cert-card__button').at(0).simulate('click');
                expect(document.querySelectorAll('.cert-card__action-error-tooltip').length).toBe(1);

                // No tooltip
                wrapper.instance().handleTooltipOutsideClick();
                expect(document.querySelectorAll('.cert-card__action-error-tooltip').length).toBe(0);
            })
            .finally(() => {
                wrapper.unmount();
            });
    });

    describe('abc service', () => {
        it('should change its abc-service', async () => {
            const requestUpdateAbcService = jest.fn();

            dataAbc.requestUpdateAbcService = requestUpdateAbcService;
            const wrapper = mount(
                <CertCardContainer {...dataAbc} />,
                {context}
            );

            wrapper.instance().handleAbcServiceChange({id: 33});

            expect(requestUpdateAbcService).toHaveBeenCalledWith(0, 33);

            await wait()
                .then(() => {
                    wrapper.unmount();
                });
        });

        it('should clean up an abc-service', async () => {
            const requestUpdateAbcService = jest.fn();

            dataAbc.requestUpdateAbcService = requestUpdateAbcService;
            const wrapper = mount(
                <CertCardContainer {...dataAbc} />,
                {context}
            );

            wrapper.instance().handleAbcServiceChange();

            expect(requestUpdateAbcService).toHaveBeenCalledWith(0, '');

            await wait()
                .then(() => {
                    wrapper.unmount();
                });
        });

        it('should set up a new abc-service even though it didnt exist before', async () => {
            const requestUpdateAbcService = jest.fn();

            dataAbc.requestUpdateAbcService = requestUpdateAbcService;
            const wrapper = mount(
                <CertCardContainer {...dataAbc} />,
                {context}
            );

            wrapper.instance().handleAbcServiceChange({id: 22});

            expect(requestUpdateAbcService).toHaveBeenCalledWith(0, 22);

            await wait()
                .then(() => {
                    wrapper.unmount();
                });
        });

        it('should not make a request if new id is the same as old', async () => {
            const requestUpdateAbcService = jest.fn();

            dataAbc.requestUpdateAbcService = requestUpdateAbcService;
            dataAbc.cert.abc_service = {id: 33};

            const wrapper = mount(
                <CertCardContainer {...dataAbc} />,
                {context}
            );

            wrapper.instance().handleAbcServiceChange({id: 33});

            expect(requestUpdateAbcService).not.toHaveBeenCalled();

            await wait()
                .then(() => {
                    wrapper.unmount();
                });
        });

        it('should show an error tooltip in case of error', () => {
            function requestUpdateAbcService() {
                throw new Error('error');
            }

            const PatchedCertCardContainer = inherit(CertCardContainer, {
                async handleAbcServiceChange() {
                    try {
                        await this.__base(...arguments);
                    } catch (e) {
                        // Noop
                    }
                }
            });

            dataAbc.cert.type = 'host';
            dataAbc.requestUpdateAbcService = requestUpdateAbcService;
            const wrapper = mount(
                <PatchedCertCardContainer {...dataAbc} />,
                {
                    context
                }
            );

            return wait()
                .then(() => {
                    wrapper.update();

                    // No tooltip
                    expect(document.querySelectorAll('.tooltip.popup2_visible_yes').length).toBe(0);

                    // Tooltip exists
                    wrapper.instance().handleAbcServiceChange({id: 22});
                    expect(document.querySelectorAll('.tooltip.popup2_visible_yes').length).toBe(1);

                    // No tooltip
                    wrapper.instance().handleAbcServiceTooltipOutsideClick();
                    expect(document.querySelectorAll('.tooltip.popup2_visible_yes').length).toBe(0);
                })
                .finally(() => {
                    wrapper.unmount();
                });
        });
    });
});
