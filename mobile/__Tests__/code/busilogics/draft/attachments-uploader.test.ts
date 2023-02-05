import { FailingAttachmentsUploader } from '../../../../code/busilogics/draft/attachments-uploader'

describe(FailingAttachmentsUploader, () => {
  const uploader = new FailingAttachmentsUploader()

  it('should reject for uploadToDisk', (done) => {
    expect.assertions(1)
    uploader.uploadToDisk({} as any).failed((e) => {
      expect(e.message).toBe('"uploadToDisk" is not implemented')
      done()
    })
  })

  it('should reject for uploadOrdinaryAttach', (done) => {
    expect.assertions(1)
    uploader.uploadOrdinaryAttach({} as any).failed((e) => {
      expect(e.message).toBe('"uploadOrdinaryAttach" is not implemented')
      done()
    })
  })
})
