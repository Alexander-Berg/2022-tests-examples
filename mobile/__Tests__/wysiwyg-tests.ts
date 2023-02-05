import * as assert from 'assert'
import { WysiwygModel } from '../code/mail/model/compose/wysiwyg-model'

describe('Wysiwyg unit tests', () => {
  it('should compose correct body', (done) => {
    const wysiwyg = new WysiwygModel()
    wysiwyg.appendText(0, '0123456789')
    wysiwyg.setItalic(0, 3)
    wysiwyg.setStrong(2, 7)
    assert.strictEqual(wysiwyg.getBody(), '<em>01<strong>2</em>3456</strong>789')
    done()
  })
  it('should add styled text', (done) => {
    const wysiwyg = new WysiwygModel()
    wysiwyg.appendText(0, '0123456789')
    wysiwyg.setItalic(0, 3)
    wysiwyg.setStrong(2, 7)
    wysiwyg.appendText(3, '777')
    assert.strictEqual(wysiwyg.getBody(), '<em>01<strong>2777</em>3456</strong>789')
    done()
  })
  it('should partly clear formatting', (done) => {
    const wysiwyg = new WysiwygModel()
    wysiwyg.appendText(0, '0123456789')
    wysiwyg.setItalic(0, 3)
    wysiwyg.setStrong(2, 7)
    wysiwyg.clearFormatting(2, 4)
    assert.strictEqual(wysiwyg.getBody(), '<em>01</em>23<strong>456</strong>789')
    done()
  })
  it('should fully clear formatting', (done) => {
    const wysiwyg = new WysiwygModel()
    wysiwyg.appendText(0, '0123456789')
    wysiwyg.setItalic(0, 3)
    wysiwyg.setStrong(2, 7)
    wysiwyg.clearFormatting(0, 7)
    assert.strictEqual(wysiwyg.getBody(), '0123456789')
    done()
  })
})
