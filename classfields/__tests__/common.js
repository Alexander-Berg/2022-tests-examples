import React from 'react';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientProfileLinks from '../index';
import styles from '../ClientProfilePipeDriveLink/styles.module.css';

export const defaultStoreMock = {
    config: {
        lkOrigin: 'https://realty.yandex.ru'
    },
    client: {
        common: {
            data: {
                id: '1337'
            }
        },
        profile: {
            data: {
                links: {
                    site: 'sitelink',
                    feeds: 'feedslink',
                    verba: 'verbalink',
                    pd: 'pdlink',
                    moderation: 'moderationlink',
                    balance: 'balancelink'
                },
                adAgency: {
                    uid: '1338'
                }
            }
        }
    }
};

export const clientId = defaultStoreMock.client.common.data.id;
export const adAgencyId = defaultStoreMock.client.profile.data.adAgency.uid;
export const links = defaultStoreMock.client.profile.data.links;

export const correctSelfLkLink = `https://realty.yandex.ru/management-new/?vos_user_login=${clientId}&moderator_mode_required`;
export const correctAdAgencyLkLink = `https://realty.yandex.ru/management-new/?vos_user_login=${adAgencyId}&moderator_mode_required`;

export const pdInputSelector = `.${styles.input} input`;
export const pdCancelButtonSelector = `.${styles.button}:last-of-type`;
export const pdSaveButtonSelector = `.${styles.button}:first-of-type`;
export const pdEditButtonSelector = `.${styles.icon}`;

export const typedText = '321';

export const Component = ({ links: propsLinks, mock }) => (
    <AppProviders store={mock}>
        <ClientProfileLinks links={propsLinks} />
    </AppProviders>
);
