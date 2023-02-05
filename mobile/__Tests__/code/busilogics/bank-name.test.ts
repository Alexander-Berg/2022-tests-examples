import { BankName, getAllVisibleBankNames, stringToBankName } from '../../../code/busilogics/bank-name'

describe(stringToBankName, () => {
  it('Unknown bank', () => {
    expect(stringToBankName('Evli Bank')).toBe(BankName.UnknownBank)
  })
  it('Null bank', () => {
    expect(stringToBankName(null)).toBe(BankName.UnknownBank)
  })
  it('Known banks', () => {
    expect(stringToBankName('AlfaBank')).toBe(BankName.AlfaBank)
    expect(stringToBankName('SberBank')).toBe(BankName.SberBank)
    expect(stringToBankName('Tinkoff')).toBe(BankName.Tinkoff)
    expect(stringToBankName('Vtb')).toBe(BankName.Vtb)
    expect(stringToBankName('GazpromBank')).toBe(BankName.GazpromBank)
    expect(stringToBankName('BankOfMoscow')).toBe(BankName.BankOfMoscow)
    expect(stringToBankName('OpenBank')).toBe(BankName.OpenBank)
    expect(stringToBankName('PromsvyazBank')).toBe(BankName.PromsvyazBank)
    expect(stringToBankName('RosBank')).toBe(BankName.RosBank)
    expect(stringToBankName('Qiwi')).toBe(BankName.Qiwi)
    expect(stringToBankName('CitiBank')).toBe(BankName.CitiBank)
    expect(stringToBankName('UnicreditBank')).toBe(BankName.UnicreditBank)
    expect(stringToBankName('RaiffeisenBank')).toBe(BankName.RaiffeisenBank)
  })
})

describe(getAllVisibleBankNames, () => {
  it('get all visible banks', () => {
    expect(getAllVisibleBankNames()).toEqual([
      BankName.AlfaBank,
      BankName.GazpromBank,
      BankName.OpenBank,
      BankName.PromsvyazBank,
      BankName.RaiffeisenBank,
      BankName.RosBank,
      BankName.SberBank,
      BankName.Tinkoff,
      BankName.UnicreditBank,
      BankName.Vtb,
      BankName.UnknownBank,
    ])
  })
})
