import { Contact } from '../../../../mapi/code/api/entities/contact/contact'
import { range, Throwing } from '../../../../../common/ys'
import {
  assertBooleanEquals,
  assertInt32Equals,
  assertStringEquals,
  assertTrue,
} from '../../../../testopithecus-common/code/utils/assert'
import { App, MBTComponent, MBTComponentType } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import {
  ComposeBodyFeature,
  ComposeSenderSuggestFeature,
  ComposeRecipientFieldsFeature,
  ComposeRecipientFieldType,
  ComposeSubjectFeature,
  ComposeRecipientSuggestFeature,
  Yabble,
} from '../feature/compose/compose-features'
import { TabBarComponent } from './tab-bar-component'

export class ComposeComponent implements MBTComponent {
  public static readonly type: MBTComponentType = 'ComposeComponent'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const composeRecipientFieldsModel = ComposeRecipientFieldsFeature.get.castIfSupported(model)
    const composeRecipientFieldsApp = ComposeRecipientFieldsFeature.get.castIfSupported(application)
    if (composeRecipientFieldsModel !== null && composeRecipientFieldsApp !== null) {
      const isCcBccFromFieldsShownModel = composeRecipientFieldsModel.isExtendedRecipientFormShown()
      const isCcBccFromFieldsShownApp = composeRecipientFieldsApp.isExtendedRecipientFormShown()
      assertBooleanEquals(
        isCcBccFromFieldsShownModel,
        isCcBccFromFieldsShownApp,
        'Cc/Bcc/From fields showing status is incorrect',
      )

      if (isCcBccFromFieldsShownModel) {
        for (const field of [
          ComposeRecipientFieldType.to,
          ComposeRecipientFieldType.cc,
          ComposeRecipientFieldType.bcc,
        ]) {
          const fieldValueModel = composeRecipientFieldsModel.getRecipientFieldValue(field)
          const fieldValueApp = composeRecipientFieldsApp.getRecipientFieldValue(field)
          assertInt32Equals(
            fieldValueModel.length,
            fieldValueApp.length,
            `Different number of yabbles in ${field} field`,
          )
          for (const i of range(0, fieldValueModel.length)) {
            const fieldModel = fieldValueModel[i]
            const fieldApp = fieldValueModel[i]
            assertTrue(
              Yabble.matches(fieldModel, fieldApp),
              `Different yabble in field ${ComposeRecipientFieldType.to.toString()} at index ${i}. Model: ${fieldModel.tostring()}, app: ${fieldApp.tostring()}`,
            )
          }
        }

        const fromFieldValueModel = composeRecipientFieldsModel.getSenderFieldValue()
        const fromFieldValueApp = composeRecipientFieldsApp.getSenderFieldValue()
        assertStringEquals(fromFieldValueModel, fromFieldValueApp, 'Incorrect From field value')
      } else {
        const compactRecipientsFieldValueModel = composeRecipientFieldsModel.getCompactRecipientFieldValue()
        const compactRecipientsFieldValueApp = composeRecipientFieldsApp.getCompactRecipientFieldValue()
        assertStringEquals(
          compactRecipientsFieldValueModel,
          compactRecipientsFieldValueApp,
          'Incorrect compact recipients field value',
        )
      }
    }

    const toCcBccSuggestModel = ComposeRecipientSuggestFeature.get.castIfSupported(model)
    const toCcBccSuggestApp = ComposeRecipientSuggestFeature.get.castIfSupported(application)
    if (toCcBccSuggestModel !== null && toCcBccSuggestApp !== null) {
      const isToCcBccSuggestShownModel = toCcBccSuggestModel.isRecipientSuggestShown()
      const isToCcBccSuggestShownApp = toCcBccSuggestApp.isRecipientSuggestShown()

      assertBooleanEquals(
        isToCcBccSuggestShownModel,
        isToCcBccSuggestShownApp,
        'To/cc/bcc suggest showing state is incorrect',
      )

      if (isToCcBccSuggestShownModel) {
        const toCcBccSuggestsModel = toCcBccSuggestModel.getRecipientSuggest()
        const toCcBccSuggestsApp = toCcBccSuggestApp.getRecipientSuggest()

        assertInt32Equals(
          toCcBccSuggestsModel.length,
          toCcBccSuggestsApp.length,
          'Incorrect number of to/cc/bcc suggests',
        )

        for (const i of range(0, toCcBccSuggestsModel.length)) {
          const suggestModel = toCcBccSuggestsModel[i]
          const suggestApp = toCcBccSuggestsApp[i]
          assertTrue(
            Contact.matches(suggestModel, suggestApp),
            `Different From suggest at ${i} index. Model: ${suggestModel.tostring()}, app: ${suggestApp.tostring()}`,
          )
        }
      }
    }

    const fromSuggestModel = ComposeSenderSuggestFeature.get.castIfSupported(model)
    const fromSuggestApp = ComposeSenderSuggestFeature.get.castIfSupported(application)
    if (fromSuggestModel !== null && fromSuggestApp !== null) {
      const isFromSuggestShownModel = fromSuggestModel.isSenderSuggestShown()
      const isFromSuggestShownApp = fromSuggestApp.isSenderSuggestShown()

      assertBooleanEquals(isFromSuggestShownModel, isFromSuggestShownApp, 'From suggest showing state is incorrect')

      if (isFromSuggestShownModel) {
        const fromSuggestsModel = fromSuggestModel.getSenderSuggest()
        const fromSuggestsApp = fromSuggestApp.getSenderSuggest()

        assertInt32Equals(fromSuggestsModel.length, fromSuggestsApp.length, 'Incorrect number of From suggests')
        for (const fromSuggestModel of fromSuggestsModel) {
          assertTrue(fromSuggestsApp.includes(fromSuggestModel), `There is no ${fromSuggestModel} suggest in app`)
        }
      }
    }

    const subjectModel = ComposeSubjectFeature.get.castIfSupported(model)
    const subjectApp = ComposeSubjectFeature.get.castIfSupported(application)
    if (subjectModel !== null && subjectApp !== null) {
      const subjModel = subjectModel.getSubject()
      const subjApp = subjectApp.getSubject()
      assertStringEquals(subjModel, subjApp, 'Incorrect subject')
    }

    const bodyModel = ComposeBodyFeature.get.castIfSupported(model)
    const bodyApp = ComposeBodyFeature.get.castIfSupported(application)
    if (bodyModel !== null && bodyApp !== null) {
      const bModel = bodyModel.getBody()
      const bApp = bodyApp.getBody()
      assertStringEquals(bModel, bApp, 'Incorrect body')
    }

    await new TabBarComponent().assertMatches(model, application)
  }

  public tostring(): string {
    return this.getComponentType()
  }

  public getComponentType(): MBTComponentType {
    return ComposeComponent.type
  }
}
