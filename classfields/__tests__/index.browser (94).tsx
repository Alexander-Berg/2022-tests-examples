import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { FlatNotificationType, IFlatNotification, TodoItemType, TenantSearchStatsActivityLevel } from 'types/flat';

import { ILegacyUser } from 'types/user';

import { AppProvider } from 'view/libs/test-helpers';
import { userReducer } from 'view/entries/user/reducer';
import { getNotificationData } from 'view/libs/notifications';

import { UserFlatNotification } from '../';

import { store } from './stubs/store';
import { flat } from './stubs/flat';

const viewportSets = [
    { viewport: { width: 375, height: 300 }, isMobile: true },
    { viewport: { width: 1024, height: 300 }, isMobile: false },
] as const;

const Component: React.FunctionComponent<{ notification: IFlatNotification; isMobile: boolean }> = (props) => {
    const { notification, isMobile } = props;
    const notificationData = getNotificationData({
        notification,
        flat,
        user: store.user as ILegacyUser,
        realtyUrl: 'https://realty.test.vertis.yandex.ru/',
    });

    if (!notificationData) {
        return null;
    }

    return (
        <AppProvider
            initialState={{ ...store, config: { ...store.config, isMobile } }}
            rootReducer={userReducer}
            bodyBackgroundColor={AppProvider.PageColor.USER_LK}
        >
            <UserFlatNotification notificationData={notificationData} flat={flat} isMobile={isMobile} />
        </AppProvider>
    );
};

describe('UserFlatNotification', () => {
    [
        'ownerWithoutCard',
        'ownerWithManyCards',
        'ownerWithoutInn',
        'ownerRequestDeclined',
        'ownerRentIsFinished',

        'draftNeedToFinish',
        'draftNeedConfirmation',
        'preparingFlatForExposition',
        'waitingForArendaTeamContact',
        'checkTenantCandidates',
        'lookingForTenants',
        'ownerRequestCanceledByOwner',
        'ownerFlatIsRented',
        'ownerPrepareFlatForMeeting',

        'frontendTenantDefault',
        'tenantSearchStats',

        'keysStillWithYou',
        'keysStillWithManager',
        'keysHandedOverToManager',

        'ownerRentWaitingForPaymentDate',
        'ownerRentHoldingForPaymentDate',
        'ownerRentExpectingPayment',
        'ownerRentWaitingForPayout',
        'ownerRentCardUnavailable',
        'ownerRentPayoutBroken',
        'ownerRentPaidOutToCard',
        'ownerRentPaidOutToAccount',
        'ownerHouseServiceSettingsConfigurationRequired',
        'ownerHouseServiceSettingsConfigurationIncomplete',
        'ownerHouseServiceReceivedMeterReadings',
        'ownerHouseServiceReceivedAllMeterReadings',
        'ownerHouseServiceTimeToSendBills',
        'ownerHouseServiceBillsPaid',
        'ownerHouseServiceBillsDeclined',
        'ownerHouseServiceReceiptsReceived',
        'ownerHouseServicePaymentConfirmationReceived',
        'ownerSignRentContract',
        'ownerRentContractChangesRequested',
        'ownerRentContractSignedByOwner',
        'ownerRentContractIsActive',

        'ownerNeedToConfirmInventory',
        'ownerNeedToFillOutInventory',

        'tenantRentEnded',
        'tenantRentFirstPayment',
        'tenantRentReadyToPay',
        'tenantRentPaymentToday',
        'tenantRentPaymentOutdated',
        'tenantRentPaid',
        'tenantHouseServiceSettingsAcceptanceRequired',
        'tenantHouseServiceSendMeterReadings',
        'tenantHouseServiceMeterReadingsDeclined',
        'tenantHouseServiceBillsReceived',
        'tenantHouseServiceTimeToSendReceipts',
        'tenantRentContractIsActive',
        'tenantHouseServiceReceiptsDeclined',
        'tenantHouseServiceTimeToSendPaymentConfirmation',
        'tenantHouseServicePaymentConfirmationDeclined',
        'tenantNeedToConfirmInventory',
    ].forEach((notificationName) => {
        it(notificationName, async () => {
            const notification = {
                [FlatNotificationType[notificationName]]: {
                    hasActualPayment: true,
                    period: '2022-01',
                    paymentInfo: {
                        amountKopecks: 3600000,
                        overdueDays: 2,
                        periodDateFrom: '2021-12-28',
                        periodDateTo: '2022-01-27',
                        remainingDays: 15,
                    },
                    paidToDate: '2022-01-27',
                },
            } as IFlatNotification;

            if (notificationName === 'tenantRentReadyToPay') {
                notification[FlatNotificationType[notificationName]]!.termsForAcceptance = {
                    version: 3,
                    contractTermsUrl: 'https://yandex.ru',
                };
            }

            if (notificationName === 'ownerPaymentInfoTodo') {
                notification[FlatNotificationType[notificationName]] = {
                    items: [
                        {
                            [TodoItemType.addInn]: {},
                            done: false,
                            error: '',
                        },
                        {
                            [TodoItemType.addPaymentCard]: {},
                            done: false,
                            error: '',
                        },
                    ],
                };
            }

            if (notificationName === 'tenantSearchStats') {
                notification[FlatNotificationType[notificationName]] = {
                    daysInExposition: 1398,
                    views: 398,
                    calls: 89,
                    showings: 90,
                    offerId: '323423412312451134',
                    applications: 38554,
                    activityLevel: TenantSearchStatsActivityLevel.NORMAL,
                    currentRentalValue: '3000',
                    currentAdValue: '3300',
                };
            }

            if (notificationName === 'ownerConfirmedTodo') {
                notification[FlatNotificationType[notificationName]] = {
                    items: [
                        {
                            [TodoItemType.addFlatInfo]: {},
                            done: false,
                            error: '',
                        },
                        {
                            [TodoItemType.addFlatPhotos]: {},
                            done: false,
                            error: '',
                        },
                        {
                            [TodoItemType.addPassport]: {},
                            done: false,
                            error: '',
                        },
                    ],
                };
            }

            for (const viewportSet of viewportSets) {
                await _render(<Component notification={notification} isMobile={viewportSet.isMobile} />, {
                    viewport: viewportSet.viewport,
                });

                expect(
                    await takeScreenshot({
                        fullPage: true,
                    })
                ).toMatchImageSnapshot();
            }
        });
    });
});
