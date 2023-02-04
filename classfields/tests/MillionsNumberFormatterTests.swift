//
//  MillionsNumberFormatterTests.swift
//  YandexRealty
//
//  Created by Mikhail Solodovnichenko on 7/21/17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
import YREFormatters

class MillionsNumberFormatterTests: XCTestCase {
    func testCommonFormats() {
        let formatter = MillionsPriceNumberFormatter()
        formatter.allowLossyTruncation = false
        
        let decimalDel = formatter.decimalSeparator
        let groupingDel = formatter.groupingSeparator
        
        let thousandsSfx = formatter.thousandsSuffix
        let millionsSfx = formatter.millionsSuffix
        let billionsSfx = formatter.billionsSuffix

        XCTAssertEqual(formatter.string(fromNumber: -700), "-700")
        XCTAssertEqual(formatter.string(fromNumber: 0), "0")
        XCTAssertEqual(formatter.string(fromNumber: 5), "5")
        XCTAssertEqual(formatter.string(fromNumber: 10), "10")
        XCTAssertEqual(formatter.string(fromNumber: 42), "42")
        XCTAssertEqual(formatter.string(fromNumber: 100), "100")
        XCTAssertEqual(formatter.string(fromNumber: 1000), "1" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1001), "1" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 10000), "10" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 52022), "52" + groupingDel + "022")
        XCTAssertEqual(formatter.string(fromNumber: 999900), "999" + decimalDel + "9" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999990), "999" + groupingDel + "990")
        XCTAssertEqual(formatter.string(fromNumber: 999999), "999" + groupingDel + "999")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000), "1" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000), "1" + decimalDel + "2" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000), "1" + decimalDel + "23" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000), "1" + groupingDel + "234" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_000_001), "1" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000), "999" + decimalDel + "99" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000), "999" + groupingDel + "999" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100), "999" + groupingDel + "999" + groupingDel + "100")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_000), "1" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_001), "1" + groupingDel + "000" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000_000), "1" + decimalDel + "2" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000_000), "1" + decimalDel + "23" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000_000), "1" + decimalDel + "234" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000_000), "999" + decimalDel + "99" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000_000), "999" + decimalDel + "999" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100_000),
                       "999" + groupingDel + "999" + groupingDel + "100" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_234_567_899_999),
                       "1" + groupingDel + "234" + groupingDel + "567" + groupingDel + "899" + groupingDel + "999")
    }
    
    func testCustomFormats() {
        let decimalDel = "."
        let groupingDel = "'"
        
        let thousandsSfx = "thousand"
        let millionsSfx = "million"
        let billionsSfx = "billion"

        let formatter = MillionsPriceNumberFormatter()
        formatter.decimalSeparator = decimalDel
        formatter.groupingSeparator = groupingDel
        formatter.thousandsSuffix = thousandsSfx
        formatter.millionsSuffix = millionsSfx
        formatter.billionsSuffix = billionsSfx
        formatter.forceHideStyleSuffixes = false
        formatter.allowLossyTruncation = false
        
        XCTAssertEqual(formatter.string(fromNumber: -700), "-700")
        XCTAssertEqual(formatter.string(fromNumber: 0), "0")
        XCTAssertEqual(formatter.string(fromNumber: 5), "5")
        XCTAssertEqual(formatter.string(fromNumber: 10), "10")
        XCTAssertEqual(formatter.string(fromNumber: 42), "42")
        XCTAssertEqual(formatter.string(fromNumber: 100), "100")
        XCTAssertEqual(formatter.string(fromNumber: 1000), "1" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1001), "1" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 10000), "10" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 52022), "52" + groupingDel + "022")
        XCTAssertEqual(formatter.string(fromNumber: 999900), "999" + decimalDel + "9" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999990), "999" + groupingDel + "990")
        XCTAssertEqual(formatter.string(fromNumber: 999999), "999" + groupingDel + "999")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000), "1" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000), "1" + decimalDel + "2" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000), "1" + decimalDel + "23" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000), "1" + groupingDel + "234" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_000_001), "1" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000), "999" + decimalDel + "99" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000), "999" + groupingDel + "999" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100), "999" + groupingDel + "999" + groupingDel + "100")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_000), "1" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_001), "1" + groupingDel + "000" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000_000), "1" + decimalDel + "2" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000_000), "1" + decimalDel + "23" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000_000), "1" + decimalDel + "234" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000_000), "999" + decimalDel + "99" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000_000), "999" + decimalDel + "999" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100_000),
                       "999" + groupingDel + "999" + groupingDel + "100" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_234_567_899_999),
                       "1" + groupingDel + "234" + groupingDel + "567" + groupingDel + "899" + groupingDel + "999")
    }
    
    func testHideStyles() {
        let decimalDel = "."
        let groupingDel = "'"
        
        let thousandsSfx = "thousand"
        let millionsSfx = "million"
        let billionsSfx = "billion"

        let formatter = MillionsPriceNumberFormatter()
        formatter.decimalSeparator = decimalDel
        formatter.groupingSeparator = groupingDel
        formatter.thousandsSuffix = thousandsSfx
        formatter.millionsSuffix = millionsSfx
        formatter.billionsSuffix = billionsSfx
        formatter.forceHideStyleSuffixes = true
        formatter.allowLossyTruncation = false
        
        XCTAssertEqual(formatter.string(fromNumber: -700), "-700")
        XCTAssertEqual(formatter.string(fromNumber: 0), "0")
        XCTAssertEqual(formatter.string(fromNumber: 5), "5")
        XCTAssertEqual(formatter.string(fromNumber: 10), "10")
        XCTAssertEqual(formatter.string(fromNumber: 42), "42")
        XCTAssertEqual(formatter.string(fromNumber: 100), "100")
        XCTAssertEqual(formatter.string(fromNumber: 1000), "1")
        XCTAssertEqual(formatter.string(fromNumber: 1001), "1" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 10000), "10")
        XCTAssertEqual(formatter.string(fromNumber: 52022), "52" + groupingDel + "022")
        XCTAssertEqual(formatter.string(fromNumber: 999900), "999" + decimalDel + "9")
        XCTAssertEqual(formatter.string(fromNumber: 999990), "999" + groupingDel + "990")
        XCTAssertEqual(formatter.string(fromNumber: 999999), "999" + groupingDel + "999")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000), "1")
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000), "1" + decimalDel + "2")
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000), "1" + decimalDel + "23")
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000), "1" + groupingDel + "234" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_000_001), "1" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000), "999" + decimalDel + "99")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000), "999" + groupingDel + "999" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100), "999" + groupingDel + "999" + groupingDel + "100")

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_000), "1")
        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_001), "1" + groupingDel + "000" + groupingDel + "000" + groupingDel + "001")
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000_000), "1" + decimalDel + "2")
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000_000), "1" + decimalDel + "23")
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000_000), "1" + decimalDel + "234")
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000_000), "999" + decimalDel + "99")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000_000), "999" + decimalDel + "999")
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100_000),
                       "999" + groupingDel + "999" + groupingDel + "100" + groupingDel + "000")
        XCTAssertEqual(formatter.string(fromNumber: 1_234_567_899_999),
                       "1" + groupingDel + "234" + groupingDel + "567" + groupingDel + "899" + groupingDel + "999")
    }
    
    func testStyleEquality() {
        let formatter = MillionsPriceNumberFormatter()
        formatter.allowLossyTruncation = false
        
        // single one is equal to itself
        XCTAssertEqual(formatter.isSameStyleNumbers([1]), true)
        XCTAssertEqual(formatter.isSameStyleNumbers([-1]), true)
        
        // nothing is equal to nothing
        XCTAssertEqual(formatter.isSameStyleNumbers([]), true)
        
        // at least single negative should result false
        XCTAssertEqual(formatter.isSameStyleNumbers([-1, 2, 3, 4]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([-1, -2, -3, -4]), false)
        
        // same should be same
        XCTAssertEqual(formatter.isSameStyleNumbers([1, 2, 3, 4]), true)
        XCTAssertEqual(formatter.isSameStyleNumbers([1001, 1002, 1003, 1004]), true)
        XCTAssertEqual(formatter.isSameStyleNumbers([1_000_001, 1_000_002, 1_000_003, 1_000_004]), true)
        XCTAssertEqual(formatter.isSameStyleNumbers([1_000_000_001, 1_000_000_002, 1_000_000_003, 1_000_000_004]), true)

        // different should be different
        XCTAssertEqual(formatter.isSameStyleNumbers([1, 1200, 1300, 1300]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([1100, 1, 1300, 1400]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([1_300_000, 1200, 130, 1400]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([1_300_000, 1200, 1, 1400]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([1_200_000_000, 1_300_000, 1200, 1, 1400]), false)
        XCTAssertEqual(formatter.isSameStyleNumbers([1100, 1, 1_300_000, 1_200_000_000, 1400]), false)

        // postprocessed should be equal
        // those all should be displayed as is, without styles
        XCTAssertEqual(formatter.isSameStyleNumbers([1, 1002, 1003, 1004]), true)
        XCTAssertEqual(formatter.isSameStyleNumbers([1, 1002, 1_000_003, 1456]), true)
    }
    
    func testLossyTruncation() {
        let formatter = MillionsPriceNumberFormatter()
        formatter.allowLossyTruncation = true
        
        let decimalDel = formatter.decimalSeparator
        let groupingDel = formatter.groupingSeparator
        
        let thousandsSfx = formatter.thousandsSuffix
        let millionsSfx = formatter.millionsSuffix
        let billionsSfx = formatter.billionsSuffix

        XCTAssertEqual(formatter.string(fromNumber: -700), "-700")
        XCTAssertEqual(formatter.string(fromNumber: 0), "0")
        XCTAssertEqual(formatter.string(fromNumber: 5), "5")
        XCTAssertEqual(formatter.string(fromNumber: 10), "10")
        XCTAssertEqual(formatter.string(fromNumber: 42), "42")
        XCTAssertEqual(formatter.string(fromNumber: 100), "100")
        XCTAssertEqual(formatter.string(fromNumber: 1000), "1" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1001), "1" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 10000), "10" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 52022), "52" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999900), "999" + decimalDel + "9" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999990), "999" + decimalDel + "9" + thousandsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999999), "999" + decimalDel + "9" + thousandsSfx)

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000), "1" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_000_001), "1" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000), "1" + decimalDel + "2" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000), "1" + decimalDel + "23" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000), "1" + decimalDel + "23" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000), "999" + decimalDel + "99" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000), "999" + decimalDel + "99" + millionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100), "999" + decimalDel + "99" + millionsSfx)

        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_000), "1" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_000_000_001), "1" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_200_000_000), "1" + decimalDel + "2" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_230_000_000), "1" + decimalDel + "23" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_000_000), "1" + decimalDel + "234" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_990_000_000), "999" + decimalDel + "99" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_000_000), "999" + decimalDel + "999" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 999_999_100_000), "999" + decimalDel + "999" + billionsSfx)
        XCTAssertEqual(formatter.string(fromNumber: 1_234_567_899_999), "1" + groupingDel + "234" + decimalDel + "567" + billionsSfx)
    }
    
    func testTruncationEquivalense() {
        let formatter = MillionsPriceNumberFormatter()
        
        // single one is equal to itself
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([700]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([-700]), true)
        
        // nothing is equal
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([]), true)
        
        // at least single negative should result false
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([-700, -700]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([700, -700]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([700, -700, 700]), false)
        
        // asIs should be equal to itself
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([0, 0]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([5, 5, 5]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([10, 10]), true)
        
        // thousands
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1000, 1000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1001, 1000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1100, 1120]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1100, 1123]), true)
        
        // millions
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_000_000, 1_000_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_100_000, 1_100_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_120_000, 1_123_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_120_000, 1_123_400]), true)

        // billions
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_000_000_000, 1_000_000_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_100_000_000, 1_100_000_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_120_000_000, 1_123_000_000]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_120_000_000, 1_123_400_000]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_123_000_000, 1_123_000_000]), true)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_123_000_000, 1_123_400_000]), true)

        // same thousands should not be equal to millions or billions
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_200_000, 1200]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_230_000, 1230]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_200_000_000, 1200]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_230_000_000, 1230]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_200_000_000, 1_200_000]), false)
        XCTAssertEqual(formatter.isEqualNumbersWhenTruncated([1_230_000_000, 1_230_000]), false)
    }
}
