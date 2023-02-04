import * as React from 'react';
import { shallow } from 'enzyme';

import Spinner from 'vertis-react/components/Spinner';

import { Wallet } from '../';

const defaultProps = {
    isJuridical: false,
    isVosUser: true,
    wallet: {
        status: 'loaded',
        balance: {
            balance: 100
        }
    }
};

describe('Wallet', () => {
    it('does not render the balance if a user is not a vos user', () => {
        const props = {
            ...defaultProps,
            isVosUser: false
        };

        const wrapper = shallow(<Wallet {...props} />);

        expect(wrapper.find('.wallet__balance').length).toBe(0);
    });

    it('renders balance if a user is from vos and the balance has money', () => {
        const balance = shallow(<Wallet {...defaultProps} />)
            .find('.wallet__balance')
            .childAt(0)
            .text();

        expect(balance).toBe('1\u00a0₽');
    });

    it("does not render the balance if it's value is null", () => {
        const props = {
            ...defaultProps,
            wallet: {
                ...defaultProps.wallet,
                balance: {
                    balance: null
                }
            }
        };

        const wrapper = shallow(<Wallet {...props} />);

        expect(wrapper.find('.wallet__balance').length).toBe(0);
    });

    it('renders Spinner if the status is pending', () => {
        const props = {
            ...defaultProps,
            wallet: {
                ...defaultProps.wallet,
                status: 'pending'
            }
        };

        const wrapper = shallow(<Wallet {...props} />);

        expect(wrapper.find(Spinner).length).toBe(1);
    });

    it('renders 0 if the balance is 0', () => {
        const props = {
            ...defaultProps,
            wallet: {
                ...defaultProps.wallet,
                balance: {
                    balance: 0
                }
            }
        };

        const balance = shallow(<Wallet {...props} />)
            .find('.wallet__balance')
            .childAt(0)
            .text();

        expect(balance).toBe('0\u00a0₽');
    });
});
