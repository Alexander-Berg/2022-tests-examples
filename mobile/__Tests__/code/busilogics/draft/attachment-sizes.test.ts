import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'
import { AccountType } from '../../../../../mapi/code/api/entities/account/account-type'
import { MailProvider } from '../../../../../mapi/code/api/entities/account/mail-provider'
import { int64 } from '../../../../../../common/ys'

describe(AttachmentSizes, () => {
  describe(AttachmentSizes.prototype.getMailishSizeLimit, () => {
    it('should return rambler attach size', () => {
      const attachSizes = new AttachmentSizes(AccountType.mailish, MailProvider.rambler)
      expect(attachSizes.getMailishSizeLimit()).toBe(int64(0))
    })
    it('should return other mailish attach size', () => {
      const attachSizes = new AttachmentSizes(AccountType.mailish, MailProvider.other)
      expect(attachSizes.getMailishSizeLimit()).toBe(int64(10485760))
    })
  })

  describe(AttachmentSizes.prototype.mustBeUploadedToDisk, () => {
    it('must be uploaded to disk for ATTACH_FILE_SIZE_INDEFINITE', () => {
      const attachSizes = new AttachmentSizes(AccountType.login, MailProvider.yandex)
      expect(attachSizes.mustBeUploadedToDisk(int64(-1), int64(0))).toBe(true)
    })

    describe(AccountType.login, () => {
      it('AccountType.login: must NOT be uploaded to disk for attachments that are below size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.login, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(26214400 - 1))).toBe(false)
      })

      it('AccountType.login: must be uploaded to disk for attachments that are above size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.login, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(26214400))).toBe(true)
      })
    })

    describe(AccountType.team, () => {
      it('AccountType.team: must NOT be uploaded to disk for attachments that are below size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.team, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(104857600 - 1))).toBe(false)
      })

      it('AccountType.team: must be uploaded to disk for attachments that are above size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.team, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(104857600))).toBe(true)
      })

      it('AccountType.team: must NOT be uploaded to disk for an attachment which size is below individual size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.team, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(41943040), int64(0))).toBe(false)
      })

      it('AccountType.team: must be uploaded to disk for an attachment which size is above individual size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.team, MailProvider.yandex)
        expect(attachSizes.mustBeUploadedToDisk(int64(41943040 + 1), int64(0))).toBe(true)
      })
    })

    describe(AccountType.mailish, () => {
      it('AccountType.mailish: must NOT be uploaded to disk for attachments that are below size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.mailish, MailProvider.other)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(10485760 - 1))).toBe(false)
      })

      it('AccountType.mailish: must be uploaded to disk for attachments that are above size limit', () => {
        const attachSizes = new AttachmentSizes(AccountType.mailish, MailProvider.other)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(10485760))).toBe(true)
      })

      it('AccountType.mailish: Rambler attaches must be always uploaded to disk', () => {
        const attachSizes = new AttachmentSizes(AccountType.mailish, MailProvider.rambler)
        expect(attachSizes.mustBeUploadedToDisk(int64(1), int64(0))).toBe(true)
      })
    })
  })
})
