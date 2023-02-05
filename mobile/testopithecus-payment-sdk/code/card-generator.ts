import { doubleToInt32, floorDouble, Int32, randomDouble, range } from '../../../common/ys'
import { CardPaymentSystem } from '../../payment-sdk/code/busilogics/card-payment-system'

export class BoundCard {
  public constructor(
    public readonly cardNumber: string,
    public readonly expirationMonth: string,
    public readonly expirationYear: string,
    public readonly cvv: string,
  ) {}

  public static generated(type: CardPaymentSystem = CardPaymentSystem.MasterCard): BoundCard {
    return new BoundCard(
      CardGenerator.generateCardNumber(type),
      BoundCardConstants.EXPIRATION_MONTH,
      CardGenerator.generateExpirationYear(),
      BoundCardConstants.CVV,
    )
  }
}

export class BoundCardConstants {
  public static readonly EXPIRATION_MONTH: string = '11'
  public static readonly CVV: string = '123'
}

export class CardGenerator {
  public static generateCardNumber(type: CardPaymentSystem): string {
    let pos: Int32 = 6
    const cardArr: Int32[] = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

    switch (type) {
      case CardPaymentSystem.MasterCard:
        cardArr[0] = 5
        cardArr[1] = 1
        break
      case CardPaymentSystem.AmericanExpress:
        cardArr[0] = 3
        cardArr[1] = 4
        break
      case CardPaymentSystem.Maestro:
        cardArr[0] = 5
        cardArr[1] = 0
        break
      case CardPaymentSystem.MIR:
        cardArr[0] = 2
        cardArr[1] = 2
        break
      case CardPaymentSystem.VISA:
        cardArr[0] = 4
        cardArr[1] = 2
        break
      default:
        break
    }

    let sum: Int32 = 0
    let final_digit: Int32 = 0
    let t = doubleToInt32(floorDouble(randomDouble() * 5)) % 5
    let len_offset: Int32 = 0
    const len: Int32 = 16

    while (pos < len - 1) {
      cardArr[pos] = doubleToInt32(floorDouble(randomDouble() * 10)) % 10
      pos = pos + 1
    }

    len_offset = (len + 1) % 2
    for (const index of range(0, len - 1)) {
      if ((index + len_offset) % 2 > 0) {
        t = cardArr[index] * 2
        if (t > 9) {
          t = t - 9
        }
        sum = sum + t
      } else {
        sum = sum + cardArr[index]
      }
    }

    final_digit = (10 - (sum % 10)) % 10
    cardArr[len - 1] = final_digit
    let cardStr = ''
    for (const index of range(0, cardArr.length)) {
      cardStr = cardStr + `${cardArr[index]}`
    }
    return cardStr
  }

  public static generateExpirationYear(): string {
    const currentYear = new Date().getFullYear() % 100
    return `${currentYear + 1}`
  }

  public static generateExpirationDate(type: ExpirationDateType): string {
    const currentYear = new Date().getFullYear() % 100
    const currentMonth = new Date().getMonth() + 1
    const formattedCurrentMonth = this.formatExpirationMonth(currentMonth)

    switch (type) {
      case ExpirationDateType.currentMonthAndYear:
        return `${formattedCurrentMonth}${currentYear}`
      case ExpirationDateType.date50YearsInFuture:
        return `${formattedCurrentMonth}${currentYear + 50}`
      case ExpirationDateType.dateMore50YearsInFuture:
        return `${formattedCurrentMonth}${currentYear + 51}`
      case ExpirationDateType.nextMonth:
        return currentMonth === 12
          ? `01${currentYear + 1}`
          : `${this.formatExpirationMonth(currentMonth + 1)}${currentYear}`
      case ExpirationDateType.previousMonth:
        return currentMonth === 1
          ? `12${currentYear - 1}`
          : `${this.formatExpirationMonth(currentMonth - 1)}${currentYear}`
      case ExpirationDateType.previousYear:
        return `${formattedCurrentMonth}${currentYear - 1}`
      case ExpirationDateType.nonExistentMonth:
        return `13${currentYear}`
      case ExpirationDateType.tooManySymbols:
        return `${formattedCurrentMonth}${currentYear}1`
    }
  }

  private static formatExpirationMonth(month: Int32): string {
    return `${month}`.length === 1 ? `0${month}` : `${month}`
  }
}

export enum ExpirationDateType {
  currentMonthAndYear,
  nextMonth,
  previousMonth,
  previousYear,
  date50YearsInFuture,
  dateMore50YearsInFuture,
  nonExistentMonth,
  tooManySymbols,
}

export class SpecificCards {
  public static readonly masterCard: BoundCard = new BoundCard('5100008498698746', '12', '50', '123')
  public static readonly mir: BoundCard = new BoundCard('2200003680082987', '12', '50', '123')
  public static readonly visa: BoundCard = new BoundCard('4200006115699289', '12', '50', '123')
}
