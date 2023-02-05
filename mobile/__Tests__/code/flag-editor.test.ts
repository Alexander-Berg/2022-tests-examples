import { RealJSONSerializerWrapper } from '../../../common/__tests__/__helpers__/mock-patches'
import { FallbackFlagEditor, FlagDeveloperSettingsEditor } from '../../code/flag-editor'
import { MockSharedPreferences } from '../../../common/__tests__/__helpers__/preferences-mock'
import { FlagsDeveloperSettings } from '../../code/flags-developer-settings'
import { Int32Flag } from '../../code/flag'

describe(FallbackFlagEditor, () => {
  it('should do nothing', () => {
    const editor = new FallbackFlagEditor()
    editor.setEditorValue('value')
    expect(editor.getCachedValue()).toBeNull()
    expect(editor.getEditorValue()).toBeNull()
  })
})

describe(FlagDeveloperSettingsEditor, () => {
  it('should set values', () => {
    const mockPrefs = new MockSharedPreferences(new Map())
    const devSettings = new FlagsDeveloperSettings(mockPrefs, RealJSONSerializerWrapper())
    devSettings.initValues()

    const intFlag = new Int32Flag('int flag', 0)
    const editor = new FlagDeveloperSettingsEditor(devSettings, intFlag)

    expect(editor.getCachedValue()).toBeNull()
    expect(editor.getEditorValue()).toBeNull()

    editor.setEditorValue(100)
    expect(editor.getCachedValue()).toBeNull()
    expect(editor.getEditorValue()).toBe(100)

    devSettings.initValues()

    expect(editor.getCachedValue()).toBe(100)
    expect(editor.getEditorValue()).toBe(100)

    editor.setEditorValue(null)
    expect(editor.getCachedValue()).toBe(100)
    expect(editor.getEditorValue()).toBeNull()

    devSettings.initValues()

    expect(editor.getCachedValue()).toBeNull()
    expect(editor.getEditorValue()).toBeNull()
  })
})
