import React from 'react';
import initStoryshots, { multiSnapshotWithOptions } from '@storybook/addon-storyshots';
import 'intersection-observer';

import { ensureRoot } from '../.storybook/bin';
import { initialize } from '../.storybook/registry.desktop';

const IntlPolyfill = jest.requireActual('intl');
jest.requireActual('intl/locale-data/jsonp/ru');

if (global.Intl) {
    Intl.NumberFormat = IntlPolyfill.NumberFormat;
    Intl.DateTimeFormat = IntlPolyfill.DateTimeFormat;
} else {
    global.Intl = IntlPolyfill;
}

jest.mock('common/lego-components', () => {
    const Spin = props => <lego-components-spin {...props} />;
    const UserPic = props => <lego-components-userpic {...props} />;
    const Checkbox = props => <lego-components-checkbox {...props} />;
    const Radio = props => <lego-components-radio {...props} />;
    const Radiobox = props => <lego-components-radiobox {...props} />;
    const RadioButton = props => <lego-components-radiobutton {...props} />;
    const Button = props => <lego-components-button {...props} />;
    const Icon = props => <lego-components-icon {...props} />;
    const Textinput = props => <lego-components-textinput {...props} />;
    const Modal = props => <lego-components-modal {...props} />;
    const Select = props => <lego-components-select {...props} />;
    const Popup = props => <lego-components-popup {...props} />;
    const Suggest = props => <lego-components-suggest {...props} />;
    const TabsMenu = props => <lego-components-tabsmenu {...props} />;
    const Textarea = props => <lego-components-textarea {...props} />;
    const Menu = props => <lego-components-menu {...props} />;
    const Tumbler = props => <lego-components-tumbler {...props} />;
    const Tooltip = props => <lego-components-tooltip {...props} />;

    return {
        Button,
        Checkbox,
        Popup,
        Icon,
        Modal,
        Radio,
        Radiobox,
        RadioButton,
        Select,
        Spin,
        Suggest,
        TabsMenu,
        Textarea,
        Textinput,
        UserPic,
        Menu,
        Tumbler,
        Tooltip
    };
});

jest.mock('@material-ui/lab/TreeView', () => {
    return props => <mui-tree-view {...props} />;
});

jest.mock('@material-ui/lab/TreeItem', () => {
    return props => <mui-tree-item {...props} />;
});

jest.mock('@material-ui/icons/Clear', () => {
    return props => <mui-icon-clear {...props} />;
});

jest.mock('user/components/Header/useAccounts', () => ({
    useAccounts: () => ({
        accounts: [],
        canAddMore: false,
        defaultAccount: undefined
    })
}));

initialize();

ensureRoot();

initStoryshots({
    suite: 'FileProperties',
    test: multiSnapshotWithOptions({})
});
