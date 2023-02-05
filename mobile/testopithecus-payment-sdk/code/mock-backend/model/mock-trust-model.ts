import { MerchantInfo } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/merchant-info'
import { Acquirer } from '../../../../payment-sdk/code/network/mobile-backend/entities/init/acquirer'
import { Int32, Nullable, range, undefinedToNull, YSError } from '../../../../../common/ys'
import { getVoid, Result, resultError, resultValue } from '../../../../common/code/result/result'
import { BankName, getAllVisibleBankNames, stringToBankName } from '../../../../payment-sdk/code/busilogics/bank-name'
import { CardPaymentSystemChecker } from '../../../../payment-sdk/code/busilogics/card-payment-system'
import {
  EnabledPaymentMethod,
  FamilyInfo,
  FamilyInfoFrame,
  PaymentMethod,
} from '../../../../payment-sdk/code/network/mobile-backend/entities/methods/payment-method'
import { getMerchantInfoByAcquirer } from '../../payment-sdk-data'
import { BindingCardExtractor } from '../binding-card-extractor'
import {
  CheckPaymentData,
  FamilyInfoMode,
  InitPaymentData,
  MobPaymentError,
  MockBindingV2,
  MockCard,
  MockOrder,
  MockSuppliedMethod,
  PaymentMethodsData,
  Verification3dsState,
} from './mock-data-types'

export class MockTrustModel {
  public constructor(
    private readonly bindingCardExtractor: BindingCardExtractor,
    private readonly sbpSupport: boolean = false,
  ) {}
  private cards: Map<string, MockCard[]> = new Map<string, MockCard[]>()
  private orders: MockOrder[] = new Array<MockOrder>()
  private bindings: MockBindingV2[] = new Array<MockBindingV2>()

  private mockBankNameStartIndex: number = 0
  private mockBanks: BankName[] = getAllVisibleBankNames()
  private familyInfoMode: FamilyInfoMode = FamilyInfoMode.disabled

  public createOrder(service: Nullable<string>, isBinding: boolean = false): string {
    const order = new MockOrder(this.generateId(), service, isBinding)
    this.orders.push(order)
    return order.id
  }

  public createYaOplataOrder(amount: string, token: string, acquirer: Nullable<Acquirer>): string {
    const order = new MockOrder(this.generateId(), null, false, amount, token, acquirer)
    this.orders.push(order)
    return order.id
  }

  public checkHasPurchase(purchaseToken: string): boolean {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    return order !== null
  }

  public setupOrder(orderId: string, amount: string, force3ds: boolean): Result<string> {
    const current = undefinedToNull(this.orders.find((value) => value.id === orderId))
    if (current === null) {
      return resultError(new YSError('No order'))
    }
    current!.amount = amount
    current!.token = this.generateId()
    current!.forced3ds = force3ds
    return resultValue(current!.token)
  }

  public initPayment(
    oAuth: Nullable<string>,
    email: Nullable<string>,
    purchaseToken: string,
    service: Nullable<string>,
    checkCvn: boolean,
  ): Result<InitPaymentData> {
    if (oAuth === null && email === null) {
      // check payments backend behavior
      return resultError(new MobPaymentError('incorrect format', 'body seems to be malformed: bad email'))
    }
    if (email !== null && !this.checkEmail(email!)) {
      return resultError(new MobPaymentError('incorrect format', 'body seems to be malformed: bad email'))
    }
    const order = undefinedToNull(this.orders.find((order) => order.token === purchaseToken))
    if (order === null) {
      return resultError(new MobPaymentError('internal error', 'internal_error'))
    }

    const isYaOplata = this.isYaOplata(purchaseToken)
    if (service !== order!.service) {
      // Для Яндекс.Оплат не проверяется сервис, т.к. при создании корзины в оплатах не передается сервис-токен.
      if (!isYaOplata) {
        return resultError(new MobPaymentError('internal error', 'internal_error'))
      }
    }
    order!.checkCvn = checkCvn

    let acquirer: Nullable<Acquirer> = null
    let merchant: Nullable<MerchantInfo> = null
    const enabledMethods: EnabledPaymentMethod[] = new Array<EnabledPaymentMethod>()
    enabledMethods.push(new EnabledPaymentMethod('card'))
    if (isYaOplata) {
      acquirer = order!.acquirer!
      merchant = getMerchantInfoByAcquirer(acquirer)
    } else if (this.sbpSupport) {
      enabledMethods.push(new EnabledPaymentMethod('sbp_qr'))
    }
    return resultValue(
      new InitPaymentData(
        purchaseToken,
        order!.amount,
        new PaymentMethodsData(this.getUserCards(oAuth, checkCvn), enabledMethods),
        acquirer,
        merchant,
      ),
    )
  }

  private isYaOplata(purchaseToken: string): boolean {
    return purchaseToken.startsWith('payment:')
  }

  public paymentMethods(oAuth: Nullable<string>, checkCvn: boolean): PaymentMethodsData {
    const enabledMethods: EnabledPaymentMethod[] = new Array<EnabledPaymentMethod>()
    enabledMethods.push(new EnabledPaymentMethod('card'))
    if (this.sbpSupport) {
      enabledMethods.push(new EnabledPaymentMethod('sbp_qr'))
    }
    return new PaymentMethodsData(this.getUserCards(oAuth, checkCvn), enabledMethods)
  }

  public supplyPaymentBySbp(oAuth: Nullable<string>, purchaseToken: string): Result<string> {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return resultError(new YSError('payment_not_found'))
    }
    if (oAuth === null) {
      return resultError(new YSError('authorization_reject'))
    }
    order!.supplied = new MockSuppliedMethod('sbp_qr')
    return resultValue('success')
  }

  public supplyPaymentByStoredCard(
    oAuth: Nullable<string>,
    purchaseToken: string,
    methodId: string,
    cvn: Nullable<string>,
  ): Result<string> {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return resultError(new YSError('payment_not_found'))
    }
    if (oAuth === null) {
      return resultError(new YSError('authorization_reject'))
    }
    const card = this.findUserCardById(oAuth!, methodId)
    if (card === null) {
      return resultError(new YSError('technical_error'))
    }
    if (order!.checkCvn && card!.cvn !== cvn) {
      return resultError(new YSError('technical_error'))
    }
    order!.supplied = new MockSuppliedMethod(methodId, this.ask3ds(order!))
    return resultValue('success')
  }

  public supplyPaymentByNewCard(
    oAuth: Nullable<string>,
    purchaseToken: string,
    cardNumber: string,
    expirationMonth: string,
    expirationYear: string,
    cvn: string,
    bindCard: boolean,
  ): Result<string> {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return resultError(new YSError('payment_not_found'))
    }
    const card = new MockCard(cardNumber, expirationMonth, expirationYear, cvn, 'card-x' + this.generateId())
    if (bindCard) {
      if (oAuth === null) {
        return resultError(new YSError('authorization_reject'))
      }
      const result = this.tryBindCard(card, oAuth!)
      if (!result) {
        return resultError(new YSError('too_many_cards'))
      }
    }
    if (!order!.finished) {
      order!.supplied = new MockSuppliedMethod(card.id, this.ask3ds(order!))
    }
    return resultValue('success')
  }

  public checkPayment(purchaseToken: string): Result<CheckPaymentData> {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return resultError(new YSError('payment_not_found'))
    } else if (order!.supplied !== null) {
      if (order!.supplied!.methodId === 'sbp_qr') {
        return this.handleSbp(order!)
      }
      if (order!.supplied!.verification3ds !== Verification3dsState.not_required) {
        return this.handle3ds(order!.supplied!.verification3ds, order!.supplied!.code3ds, order!)
      }
      order!.finished = true
      // handle amounts and card ids
      const special = this.handleSpecialAmount(order!.amount)
      if (special !== null) {
        return resultError(new YSError(special))
      }
      if (order!.isBinding) {
        const result = this.handleBindingV2Payment(purchaseToken)
        if (!result) {
          return resultError(new YSError('too_many_cards'))
        }
      }
      return this.respondPaidOk(order!)
    } else {
      return resultError(new YSError('unknown_error'))
    }
  }

  private handleSbp(order: MockOrder): Result<CheckPaymentData> {
    if (!order.supplied!.sbpConfirmed) {
      return resultValue(
        new CheckPaymentData(
          'wait_for_notification',
          'in progress',
          order.token,
          order.amount,
          order.isBinding,
          Date.now().toString(),
          null,
          this.makeSbpFormUrl(order.token),
        ),
      )
    } else {
      order.finished = true
      return this.respondPaidOk(order)
    }
  }

  public bindCard(
    oAuth: string,
    cardNumber: string,
    expirationMonth: string,
    expirationYear: string,
    cvn: string,
    service: Nullable<string>,
  ): Result<string> {
    const card = new MockCard(cardNumber, expirationMonth, expirationYear, cvn, 'card-x' + this.generateId())
    const result = this.tryBindCard(card, oAuth)
    if (!result) {
      return resultError(new YSError('too_many_cards'))
    }
    return resultValue('success')
  }

  public unBindCard(oAuth: string, cardId: string): Result<string> {
    const userCards = undefinedToNull(this.cards.get(oAuth))
    if (userCards === null) {
      return resultError(new YSError('invalid_processing_request'))
    }
    const cards = userCards!
    let cardIndex: Int32 = 0
    let bound: Nullable<MockCard> = null
    for (const i of range(0, cards.length)) {
      if (cards[i].id === cardId) {
        cardIndex = i
        bound = cards[i]
      }
    }
    if (bound === null) {
      return resultError(new YSError('invalid_processing_request'))
    }
    cards.splice(cardIndex, 1)
    return resultValue('success')
  }

  public has3dsChallenge(purchaseToken: string): boolean {
    const order = this.find3dsChallenge(purchaseToken)
    if (order === null) {
      return false
    }
    return order!.supplied!.verification3ds === Verification3dsState.required
  }

  public provide3ds(purchaseToken: string, code: string): boolean {
    const order = this.find3dsChallenge(purchaseToken)
    if (order === null) {
      return false
    }
    if (order!.supplied!.verification3ds !== Verification3dsState.required) {
      return false
    }
    order!.supplied!.verification3ds = Verification3dsState.provided
    order!.supplied!.code3ds = code
    return true
  }

  public startV2Binding(oAuth: string, cardData: string, hashAlgo: string, service: string): Result<string> {
    const card = this.bindingCardExtractor.createCardFromData('card-x' + this.generateId(), cardData, hashAlgo)
    if (card.isError()) {
      return resultError(new YSError('invalid_processing_request'))
    }
    const bind = new MockBindingV2(oAuth, service, card.getValue(), this.generateId())
    this.bindings.push(bind)
    return resultValue(bind.card.id)
  }

  public verifyBinding(oAuth: string, cardId: string, service: string): Result<string> {
    const existentCard = this.findUserCardById(oAuth, cardId)
    let binding: Nullable<MockBindingV2> = undefinedToNull(this.bindings.find((value) => value.card.id === cardId))
    if (binding !== null && binding!.service !== service) {
      return resultError(new MobPaymentError('internal error', 'internal_error'))
    }
    // There are two options:
    // 1. Verify: card already bound and we want to verify it. In this case existentCard is not null and
    // no MockBindingV2 data for it.
    // 2. Bind & Verify: card sent thru /bindings endpoint before verify call and we have MockBindingV2 data,
    // but no previously added card.
    // Other cases is not correct.
    if (existentCard !== null && binding === null) {
      binding = new MockBindingV2(oAuth, service, existentCard!, '')
      this.bindings.push(binding)
    } else if (existentCard !== null || binding === null) {
      return resultError(new MobPaymentError('internal error', 'internal_error'))
    }

    // create a validating purchase
    const order = this.createOrder(service, true)
    const purchase = this.setupOrder(order, '2.0', true)
    if (purchase.isError()) {
      return resultError(new MobPaymentError('internal error', 'internal_error'))
    }
    binding!.purchaseToken = purchase.getValue()
    let res: Result<string> = resultError(new YSError('invalid_processing_request'))
    if (existentCard !== null) {
      res = this.supplyPaymentByStoredCard(oAuth, purchase.getValue(), existentCard!.id, null)
    } else {
      res = this.supplyPaymentByNewCard(
        oAuth,
        purchase.getValue(),
        binding!.card.cardNumber,
        binding!.card.expirationMonth,
        binding!.card.expirationYear,
        binding!.card.cvn,
        false,
      )
    }
    if (res.isError()) {
      return res
    }
    return resultValue(binding!.purchaseToken)
  }

  public confirmSbpPaid(purchaseToken: string): boolean {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return false
    }
    if (order!.supplied === null) {
      return false
    }
    if (order!.supplied!.methodId !== 'sbp_qr') {
      return false
    }
    order!.supplied!.sbpConfirmed = true
    return true
  }

  public setStartMockBank(bank: string): Result<void> {
    const index = this.mockBanks.lastIndexOf(stringToBankName(bank))
    if (index > this.mockBanks.length - 1 || index < 0) {
      return resultError(new MobPaymentError('internal error', 'internal_error'))
    }
    this.mockBankNameStartIndex = index
    return resultValue(getVoid())
  }

  public setFamilyInfoMode(mode: FamilyInfoMode): void {
    this.familyInfoMode = mode
  }

  private find3dsChallenge(purchaseToken: string): Nullable<MockOrder> {
    const order = undefinedToNull(this.orders.find((value) => value.token === purchaseToken))
    if (order === null) {
      return null
    }
    if (order!.supplied === null) {
      return null
    }
    return order
  }

  private getUserCards(oAuth: Nullable<string>, checkCvn: boolean): PaymentMethod[] {
    if (oAuth === null) {
      return new Array<PaymentMethod>()
    }
    const userCards = undefinedToNull(this.cards.get(oAuth))
    if (userCards === null) {
      return new Array<PaymentMethod>()
    }
    let cardIndex: Int32 = 0
    let bankIndex = this.mockBankNameStartIndex
    return userCards!.map((card) => {
      const system = CardPaymentSystemChecker.instance.lookup(card.cardNumber).toString()
      const lastDigits = card.cardNumber.slice(-4)
      const masked = lastDigits.padStart(card.cardNumber.length, '*')
      const bankName = this.mockBanks[bankIndex]
      let familyInfo: Nullable<FamilyInfo> = null
      if (cardIndex === 0) {
        // для тестов мы проставляем FamilyInfo только для первой карты пользователя
        familyInfo = this.getFamilyInfo()
      }
      bankIndex = (bankIndex + 1) % this.mockBanks.length
      cardIndex = cardIndex + 1
      return new PaymentMethod(card.id, masked, system, checkCvn, bankName, familyInfo, null)
    })
  }

  private checkEmail(email: string): boolean {
    if (email.length === 0) {
      return true
    }
    const str = email.split('')
    return str.includes('.') && str.length >= 5 && str.filter((item) => item === '@').length === 1
  }

  private generateId(): string {
    return Date.now().toString()
  }

  private ask3ds(order: MockOrder): Verification3dsState {
    // enable 3ds for special amount
    if (order.amount === '1093.00' || order.forced3ds) {
      return Verification3dsState.required
    }

    return Verification3dsState.not_required
  }

  private tryBindCard(card: MockCard, oAuth: string): boolean {
    const userCards = undefinedToNull(this.cards.get(oAuth))
    if (userCards === null) {
      const added = new Array<MockCard>()
      added.push(card)
      this.cards.set(oAuth, added)
    } else {
      const cards = userCards!
      if (cards.length === 5) {
        return false
      }
      let exists = false
      cards.forEach((value) => {
        if (value.cardNumber === card.cardNumber) {
          exists = true
        }
      })
      if (!exists) {
        cards.push(card)
      }
    }
    return true
  }

  private findUserCardById(oAuth: string, cardId: string): Nullable<MockCard> {
    const userCards = undefinedToNull(this.cards.get(oAuth))
    if (userCards !== null) {
      return undefinedToNull(userCards!.find((value) => value.id === cardId))
    }
    return null
  }

  private respondPaidOk(order: MockOrder, redirect3dsUrl: Nullable<string> = null): Result<CheckPaymentData> {
    return resultValue(
      new CheckPaymentData(
        'success',
        'paid ok',
        order.token,
        order.amount,
        order.isBinding,
        Date.now().toString(),
        redirect3dsUrl,
        null,
      ),
    )
  }

  private handleBindingV2Payment(purchaseToken: string): boolean {
    let bindIndex: Int32 = 0
    for (const i of range(0, this.bindings.length)) {
      if (this.bindings[i].purchaseToken === purchaseToken) {
        bindIndex = i
        break
      }
    }
    const result = this.tryBindCard(this.bindings[bindIndex].card, this.bindings[bindIndex].oAuth)
    this.bindings = this.bindings.slice(bindIndex, 1)
    return result
  }

  private handle3ds(state: Verification3dsState, code: string, order: MockOrder): Result<CheckPaymentData> {
    if (state === Verification3dsState.required) {
      return resultValue(
        new CheckPaymentData(
          'wait_for_notification',
          'in progress',
          order.token,
          order.amount,
          order.isBinding,
          Date.now().toString(),
          this.make3dsUrl(order.token),
          null,
        ),
      )
    }
    order.finished = true
    switch (code) {
      case '200':
        if (order.isBinding) {
          const result = this.handleBindingV2Payment(order.token)
          if (!result) {
            return resultError(new YSError('too_many_cards'))
          }
        }
        return this.respondPaidOk(order, this.make3dsUrl(order.token))
      case '400':
        return resultError(new YSError('technical_error'))
      case '401':
        return resultError(new YSError('fail_3ds'))
      case '300':
        return resultError(new YSError('not_enough_funds'))
      case '301':
        return resultError(new YSError('limit_exceeded'))
      case '302':
        return resultError(new YSError('payment_timeout'))
      case '303':
        return resultError(new YSError('technical_error'))
      case '304':
        return resultError(new YSError('limit_exceeded'))
      case '305':
        return resultError(new YSError('restricted_card'))
      case '306':
        return resultError(new YSError('transaction_not_permitted'))
      default:
        return resultError(new YSError('unknown_error'))
    }
  }

  private make3dsUrl(purchaseToken: string): string {
    // up it to configuration
    return `http://127.0.0.1:8080/web/redirect_3ds?purchase_token=${purchaseToken}`
  }

  private makeSbpFormUrl(purchaseToken: string): string {
    return `https://qr.nspk.ru/invalid/pay_sbp?purchase_token=${purchaseToken}`
  }

  private handleSpecialAmount(amount: string): Nullable<string> {
    switch (amount) {
      case '1099.00':
        return 'not_enough_funds'
      case '1092.00':
        // check
        return 'limit_exceeded'
      case '1097.00':
        return 'restricted_card'
      case '1096.00':
        return 'restricted_card'
      case '1094.00':
        return 'transaction_not_permitted'
      case '1095.00':
        return 'transaction_not_permitted'
      default:
        return null
    }
  }

  private getFamilyInfo(): Nullable<FamilyInfo> {
    const frame = FamilyInfoFrame.month.toString()

    switch (this.familyInfoMode) {
      case FamilyInfoMode.disabled:
        return null
      case FamilyInfoMode.enabled_low_allowance:
        return new FamilyInfo('1', 'f1', 99_99, 100_00, 'RUB', frame, false)
      case FamilyInfoMode.enabled_high_allowance:
        return new FamilyInfo('1', 'f1', 0, 100_00, 'RUB', frame, false)
      case FamilyInfoMode.enabled_unbound_limit:
        return new FamilyInfo('1', 'f1', 0, 1_000_000_00, 'RUB', frame, true)
      default:
        return null
    }
  }
}
