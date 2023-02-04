import React from 'react';
import { expect } from 'chai';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';

import configureStore from 'configureStore';
import App from '../index';

const INIT_DATA = {
    common: {
        data: {
            app: {
                domain: 'ru',
            },
            locale: {
                language: 'ru',
                languages: [
                    {
                        lang: 'ru',
                        name: 'Ру'
                    },
                    {
                        lang: 'en',
                        name: 'En'
                    },
                    {
                        lang: 'tr',
                        name: 'Tr'
                    }
                ],
                contentRegion: 'ru',
                regionId: 51,
            },
            hosts: {
                currentHost: 'https://developer.tech.yandex.ru',
                staticHost: '/static',
                passportHost: '//passport.yandex.ru',
            },
            user: {
                uid: '1234567890',
                login: 'apikeys',
                avatar: '0/0-0',
            },
            info: {
                token: 'q1w2e3r4t5y6',
            },
        },
    },
    error: null,
};

const store = configureStore(INIT_DATA, true);

it('renders without crashing', () => {
    const app = mount(<Provider store={store}><App /></Provider>);

    expect(app.length).to.equal(1);
});
