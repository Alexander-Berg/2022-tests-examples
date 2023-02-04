import { noop } from 'lodash';

import { DeepPartial } from 'utility-types';

import { RequestStatus } from 'realty-core/types/network';

import { UserId } from 'types/user';

import { IUniversalStore } from 'view/modules/types';
import { INavbarMenuPopupProps } from 'view/components/Navbar/NavbarMenu/NavbarMenuPopup';

export const commonProps: INavbarMenuPopupProps = {
    className: '',
    onClose: () => noop(),
    displayName: 'displayname',
    otherAccounts: [],
    isPassportFilled: true,
    userId: '4026826176' as UserId,
    isManager: false,
    isPhotographer: false,
    isOwner: false,
    isCallCenter: false,
    isCopywriter: false,
    isRetoucher: false,
    hasReviewPermission: true,
    passportUrls: {
        profile: '',
        login: '',
        getPassportEndpoint: (action: string) => action,
    },
    status: RequestStatus.LOADED,
    isVisibleAddAccount: true,
};

export const baseStore: DeepPartial<IUniversalStore> = {
    passport: {
        avatarHost: 'avatars.mdst.yandex.net',
        avatarId: '0/0-0',
    },
};

export const newProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isPassportFilled: false,
};
export const managerProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isManager: true,
};

export const ownerProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isOwner: true,
};

export const callCenterProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isCallCenter: true,
};

export const photographerProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isPhotographer: true,
};

export const copywriterProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isCopywriter: true,
};

export const retoucherProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
    isRetoucher: true,
};

export const notVisibleAddAccountButton: INavbarMenuPopupProps = {
    ...commonProps,
    isManager: true,
    isVisibleAddAccount: false,
};

export const tenantProfileProps: INavbarMenuPopupProps = {
    ...commonProps,
};

export const withoutReviewPermission: INavbarMenuPopupProps = {
    ...commonProps,
    hasReviewPermission: false,
};

export const hasOtherAccounts: INavbarMenuPopupProps = {
    ...commonProps,
    isManager: true,
    otherAccounts: [
        {
            uid: '234232323',
            displayName: {
                name: 'profile_name',
                default_avatar: '',
            },
            avatarHost: '',
        },
        {
            uid: '101011010',
            displayName: {
                name: 'profile_name',
                default_avatar: '',
            },
            avatarHost: '',
        },
    ],
};

export const getPropsWithTouch = (props: INavbarMenuPopupProps, showTouchMenu: boolean): INavbarMenuPopupProps => {
    return {
        ...props,
        showTouchMenu,
    };
};
