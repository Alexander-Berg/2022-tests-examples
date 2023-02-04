import React, { Fragment } from 'react';
import { IntlProvider, injectIntl } from 'react-intl';

import tanker from './tanker';
import paystep from './paystep';

window['balance-tanker-cat'] = tanker;
window['balance-tanker-paystep-messages'] = tanker;

let IntlWrapper = ({ intl, children }) => {
    window.balance2.intl = intl;
    return <Fragment>{children}</Fragment>;
};

IntlWrapper = injectIntl(IntlWrapper);

const withIntlProvider = Component => props => {
    return (
        <IntlProvider locale="ru" messages={tanker.ru.cat} onError={() => {}}>
            <IntlWrapper>
                <Component {...props} />
            </IntlWrapper>
        </IntlProvider>
    );
};

export default withIntlProvider;
