import { AccountType } from '../../../../../mapi/code/api/entities/account/account-type'
import { Throwing } from '../../../../../../common/ys'
import { signaturePlaceToInt32 } from '../../../../../mapi/code/api/entities/settings/settings-entities'
import { App, MBTAction, MBTComponent } from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
} from '../../../../../testopithecus-common/code/utils/assert'
import {
  CloseAccountSettingsAction,
  SwitchOffTabsAction,
  SwitchOffThreadingAction,
  SwitchOnTabsAction,
  SwitchOnThreadingAction,
} from '../../actions/settings/account-settings-actions'
import { MultiAccountFeature } from '../../feature/login-features'
import {
  AccountSettingsFeature,
  AndroidAccountSettingsFeature,
  IosAccountSettingsFeature,
} from '../../feature/settings/account-settings-feature'

export class AccountSettingsComponent implements MBTComponent {
  public static readonly type: string = 'AccountSettingsComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const accountSettingsModel = AccountSettingsFeature.get.castIfSupported(model)
    const accountSettingsApplication = AccountSettingsFeature.get.castIfSupported(application)
    const androidAccountSettingsModel = AndroidAccountSettingsFeature.get.castIfSupported(model)
    const androidAccountSettingsApplication = AndroidAccountSettingsFeature.get.castIfSupported(application)
    const iOSAccountSettingsModel = IosAccountSettingsFeature.get.castIfSupported(model)
    const iOSAccountSettingsApplication = IosAccountSettingsFeature.get.castIfSupported(application)

    if (accountSettingsModel !== null && accountSettingsApplication !== null) {
      assertBooleanEquals(
        accountSettingsModel.isGroupBySubjectEnabled(),
        accountSettingsApplication.isGroupBySubjectEnabled(),
        'Group by subject status is incorrect',
      )

      assertBooleanEquals(
        accountSettingsModel.isThemeEnabled(),
        accountSettingsApplication.isThemeEnabled(),
        'FolderList theme status is incorrect',
      )

      assertBooleanEquals(
        accountSettingsModel.isSortingEmailsByCategoryEnabled(),
        accountSettingsApplication.isSortingEmailsByCategoryEnabled(),
        'Tabs status is incorrect',
      )

      assertStringEquals(
        this.setStandartSignatureIfNeeded(accountSettingsModel.getSignature()),
        this.setStandartSignatureIfNeeded(accountSettingsApplication.getSignature()),
        'Signature is incorrect',
      )

      const applicationFolderToNotificationOption = accountSettingsApplication.getFolderToNotificationOption()
      for (const folderName of applicationFolderToNotificationOption.keys()) {
        assertStringEquals(
          applicationFolderToNotificationOption.get(folderName)!.toString(),
          accountSettingsModel.getNotificationOptionForFolder(folderName).toString(),
          `Notification option for folder ${folderName} is incorrect`,
        )
      }
    }

    if (iOSAccountSettingsModel !== null && iOSAccountSettingsApplication !== null) {
      assertStringEquals(
        iOSAccountSettingsModel.getPushNotificationSound().toString(),
        iOSAccountSettingsApplication.getPushNotificationSound().toString(),
        'Notification sound is incorrect',
      )

      assertBooleanEquals(
        iOSAccountSettingsModel.isPushNotificationForAllEnabled(),
        iOSAccountSettingsApplication.isPushNotificationForAllEnabled(),
        'Push notification for all status is incorrect',
      )
    }

    if (androidAccountSettingsModel !== null && androidAccountSettingsApplication !== null) {
      assertBooleanEquals(
        androidAccountSettingsModel.isAccountUsingEnabled(),
        androidAccountSettingsApplication.isAccountUsingEnabled(),
        'Account using status is incorrect',
      )

      assertInt32Equals(
        signaturePlaceToInt32(androidAccountSettingsModel.getPlaceForSignature()),
        signaturePlaceToInt32(androidAccountSettingsApplication.getPlaceForSignature()),
        'Place for signature is incorrect',
      )
    }
  }

  private setStandartSignatureIfNeeded(signature: string): string {
    return signature === '' ? 'Sent from Yandex Mail for mobile' : signature.substr(-32)
  }

  public getComponentType(): string {
    return AccountSettingsComponent.type
  }

  public tostring(): string {
    return this.getComponentType()
  }
}

export class AllAccountSettingsActions implements MBTComponentActions {
  public constructor(public readonly accountType: AccountType) {}

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    MultiAccountFeature.get.performIfSupported(model, (mailboxModel) => {
      actions.push(new CloseAccountSettingsAction())
      actions.push(new SwitchOnThreadingAction())
      actions.push(new SwitchOffThreadingAction())
      actions.push(new SwitchOnTabsAction(this.accountType))
      actions.push(new SwitchOffTabsAction(this.accountType))
    })
    return actions
  }
}
