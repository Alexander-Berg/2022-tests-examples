import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../testopithecus-common/code/utils/assert'
import { Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { MessageViewBackToMailListAction } from '../actions/opened-message/message-actions'
import { MessageViewerAndroidFeature, MessageViewerFeature } from '../feature/mail-view-features'
import { QuickReplyFeature, SmartReplyFeature } from '../feature/quick-reply-features'
import { LanguageName, TranslatorBarFeature } from '../feature/translator-features'
import { FullMessage } from '../model/mail-model'
import { TranslatorLanguageName } from '../model/translator-models'
import { TabBarComponent } from './tab-bar-component'

export class MessageComponent implements MBTComponent {
  public static readonly type: string = 'MessageComponent'

  public getComponentType(): string {
    return MessageComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const messageNavigatorModel = MessageViewerFeature.get.castIfSupported(model)
    const messageNavigatorApp = MessageViewerFeature.get.castIfSupported(application)
    const androidMessageNavigatorModel = MessageViewerAndroidFeature.get.castIfSupported(model)
    const androidMessageNavigatorApp = MessageViewerAndroidFeature.get.castIfSupported(application)

    if (messageNavigatorModel !== null && messageNavigatorApp !== null) {
      const openedMessageInModel = messageNavigatorModel.getOpenedMessage()
      const openedMessageInApp = messageNavigatorApp.getOpenedMessage()
      assertTrue(
        FullMessage.matches(openedMessageInModel, openedMessageInApp),
        `Opened messages are different, model: ${openedMessageInModel.tostring()}, actual: ${openedMessageInApp.tostring()}`,
      )
      const messageLabelsInModel = messageNavigatorModel.getLabels()
      const messageLabelsInApp = messageNavigatorApp.getLabels()
      for (const label of messageLabelsInModel.values()) {
        assertBooleanEquals(
          true,
          messageLabelsInApp.has(label),
          `Missing label: ${label}. Model: ${messageLabelsInModel.values()}. App: ${messageLabelsInApp.values()}`,
        )
      }
      assertTrue(
        messageLabelsInModel.size === messageLabelsInApp.size,
        `Labels are different. Model: ${messageLabelsInModel.values()}. App: ${messageLabelsInApp.values()}`,
      )
    }

    const modelTranslatorBar = TranslatorBarFeature.get.castIfSupported(model)
    const appTranslatorBar = TranslatorBarFeature.get.castIfSupported(application)
    if (modelTranslatorBar !== null && appTranslatorBar !== null) {
      const modelTranslatorBarShown = modelTranslatorBar.isTranslatorBarShown()
      const appTranslatorBarShown = appTranslatorBar.isTranslatorBarShown()

      assertBooleanEquals(modelTranslatorBarShown, appTranslatorBarShown, 'Translator bar show status is incorrect')

      if (appTranslatorBarShown) {
        let modelTargetLanguage = modelTranslatorBar.getTargetLanguage().toLowerCase()
        const appTargetLanguage = appTranslatorBar.getTargetLanguage().toLowerCase()

        let modelSourceLanguage = modelTranslatorBar.getSourceLanguage().toLowerCase()
        const appSourceLanguage = appTranslatorBar.getSourceLanguage().toLowerCase()

        if (androidMessageNavigatorApp !== null && androidMessageNavigatorModel !== null) {
          const languageMessage = androidMessageNavigatorModel.getDefaultSourceLanguage().toLowerCase()
          modelTargetLanguage = this.setLanguageInModelIfAndroid(
            modelTargetLanguage,
            appTargetLanguage,
            languageMessage,
          )
          modelSourceLanguage = this.setLanguageInModelIfAndroid(
            modelSourceLanguage,
            appSourceLanguage,
            languageMessage,
          )

          if (appSourceLanguage !== languageMessage) {
            modelSourceLanguage = appSourceLanguage
          } else if (modelSourceLanguage === modelTargetLanguage) {
            modelSourceLanguage = TranslatorLanguageName.select
          }
        }

        assertStringEquals(modelTargetLanguage, appTargetLanguage, 'Translator bar source language is incorrect')
        assertStringEquals(modelSourceLanguage, appSourceLanguage, 'Translator bar source language is incorrect')

        const modelSubmitButtonLabel = modelTranslatorBar.getSubmitButtonLabel()
        const appSubmitButtonLabel = appTranslatorBar.getSubmitButtonLabel()

        assertStringEquals(modelSubmitButtonLabel, appSubmitButtonLabel, 'Submit button label is incorrect')
      }
    }

    const modelQuickReply = QuickReplyFeature.get.castIfSupported(model)
    const appQuickReply = QuickReplyFeature.get.castIfSupported(application)
    if (modelQuickReply !== null && appQuickReply !== null) {
      const modelQuickReplyShown = modelQuickReply.isQuickReplyShown()
      const appQuickReplyShown = appQuickReply.isQuickReplyShown()

      assertBooleanEquals(modelQuickReplyShown, appQuickReplyShown, 'Quick reply show status is incorrect')

      if (appQuickReplyShown) {
        const modelTextFieldValue = modelQuickReply.getTextFieldValue()
        const appTextFieldValue = appQuickReply.getTextFieldValue()

        assertStringEquals(modelTextFieldValue, appTextFieldValue, 'Quick reply text field value is incorrect')

        if (modelTextFieldValue !== '') {
          const modelQuickReplyTextFieldExpanded = modelQuickReply.isQuickReplyTextFieldExpanded()
          const appQuickReplyTextFieldExpanded = appQuickReply.isQuickReplyTextFieldExpanded()

          assertBooleanEquals(
            modelQuickReplyTextFieldExpanded,
            appQuickReplyTextFieldExpanded,
            'Quick reply text field expand status is incorrect',
          )
        }

        const modelSendButtonEnabled = modelQuickReply.isSendButtonEnabled()
        const appSendButtonEnabled = appQuickReply.isSendButtonEnabled()

        assertBooleanEquals(modelSendButtonEnabled, appSendButtonEnabled, 'Send button enable status is incorrect')

        const modelSmartReply = SmartReplyFeature.get.castIfSupported(model)
        const appSmartReply = SmartReplyFeature.get.castIfSupported(application)
        if (modelSmartReply !== null && appSmartReply !== null) {
          const modelSmartRepliesShown = modelSmartReply.isSmartRepliesShown()
          const appSmartRepliesShown = appSmartReply.isSmartRepliesShown()

          assertBooleanEquals(modelSmartRepliesShown, appSmartRepliesShown, 'Smart replies shown status is incorrect')

          if (modelSmartRepliesShown) {
            const modelSmartReplies = modelSmartReply.getSmartReplies()
            const appSmartReplies = appSmartReply.getSmartReplies()

            assertInt32Equals(modelSmartReplies.length, appSmartReplies.length, 'Incorrect number of smart replies')

            for (const modelSmartReply of modelSmartReplies) {
              assertTrue(
                appSmartReplies.includes(modelSmartReply),
                `There is no smart reply with label ${modelSmartReply}`,
              )
            }
          }
        }
      }
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }

  private setLanguageInModelIfAndroid(
    modelLanguage: LanguageName,
    appLanguage: LanguageName,
    messageLanguage: LanguageName,
  ): LanguageName {
    return modelLanguage === 'auto' && appLanguage !== 'auto' ? messageLanguage : modelLanguage
  }
}

export class AllMessageActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new MessageViewBackToMailListAction())
    // actions.push(new ReplyMessageAction());
    return actions
  }
}
