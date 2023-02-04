package schemas

import (
	"fmt"
	"testing"

	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"github.com/stretchr/testify/suite"
)

type ReceiptTestSuite struct {
	suite.Suite
}

func TestReceiptTestSuite(t *testing.T) {
	suite.Run(t, new(ReceiptTestSuite))
}

func (s *ReceiptTestSuite) TestSupplierInfo() {
	testCases := []struct {
		supplierInfo SupplierInfo
		invalid      bool
	}{
		{supplierInfo: SupplierInfo{Inn: "1234567890", Phone: btesting.RandS(3), Name: btesting.RandS(1)}},
		{supplierInfo: SupplierInfo{Inn: "123456789012", Phone: btesting.RandS(19), Name: btesting.RandS(1)}},
		{supplierInfo: SupplierInfo{Inn: "123456789012"}},
		{supplierInfo: SupplierInfo{Phone: btesting.RandS(19), Name: btesting.RandS(1)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "1234567890", Phone: btesting.RandS(2), Name: btesting.RandS(1)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "1234567890", Phone: btesting.RandS(20), Name: btesting.RandS(1)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "1234567890", Phone: btesting.RandS(5), Name: btesting.RandS(257)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "123456789", Phone: btesting.RandS(5), Name: btesting.RandS(1)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "1234567890123", Phone: btesting.RandS(5), Name: btesting.RandS(1)}, invalid: true},
		{supplierInfo: SupplierInfo{Inn: "123456789a", Phone: btesting.RandS(5), Name: btesting.RandS(1)}, invalid: true},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.supplierInfo), func() {
			err := tc.supplierInfo.Validate()

			if tc.invalid {
				s.Assert().Error(err)
			} else {
				s.Assert().NoError(err)
			}
		})
	}
}

func (s *ReceiptTestSuite) TestRow() {
	qty := NewQuantity(1, 0)
	money := NewMoney(1504, -2)
	testCases := []struct {
		row     ReceiptRow
		invalid bool
	}{
		{row: ReceiptRow{}, invalid: true},
		{row: ReceiptRow{Qty: qty, Price: money, Text: "Some text", PaymentTypeType: PrepaymentTypeType, TaxType: NDS20}},
		{row: ReceiptRow{Qty: qty, Price: money, Text: "Some text", PaymentTypeType: PaymentTypeType("bad type"), TaxType: NDS20}, invalid: true},
		{row: ReceiptRow{Qty: qty, Price: money, Text: "Some text", PaymentTypeType: PrepaymentTypeType, TaxType: TaxType("bad tax")}, invalid: true},
		{row: ReceiptRow{Price: money, Qty: qty, TaxType: NDS20, PaymentTypeType: PrepaymentTypeType, Text: "Some text", ItemCode: "1234", AgentType: Agent, SupplierInfo: SupplierInfo{Inn: "1234567890"}}},
		{row: ReceiptRow{Price: money, Qty: qty, TaxType: NDS20, PaymentTypeType: PrepaymentTypeType, Text: "Some text", ItemCode: "1234", AgentType: AgentType("strange agent"), SupplierInfo: SupplierInfo{Inn: "1234567890"}}, invalid: true},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprint(i), func() {
			err := tc.row.Validate()

			if tc.invalid {
				s.Assert().Error(err)
			} else {
				s.Assert().NoError(err)
			}
		})
	}
}

func (s *ReceiptTestSuite) TestCompositeEventId() {
	testCases := []struct {
		compositeEventID CompositeEventID
		invalid          bool
	}{
		{compositeEventID: CompositeEventID{PaymentIDType: TrustPurchaseToken, PaymentID: "1a2b3c", EventID: "4d5e6f"}},
		{compositeEventID: CompositeEventID{PaymentIDType: TrustPurchaseToken, PaymentID: "1a2b3c"}, invalid: true},
		{compositeEventID: CompositeEventID{PaymentIDType: TrustPurchaseToken, EventID: "4d5e6f"}, invalid: true},
		{compositeEventID: CompositeEventID{PaymentID: "1a2b3c", EventID: "4d5e6f"}, invalid: true},
		{compositeEventID: CompositeEventID{PaymentIDType: PaymentIDType("bad type"), PaymentID: "1a2b3c", EventID: "4d5e6f"}, invalid: true},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprint(i), func() {
			err := tc.compositeEventID.Validate()

			if tc.invalid {
				s.Assert().Error(err)
			} else {
				s.Assert().NoError(err)
			}
		})
	}
}

func (s *ReceiptTestSuite) TestPayment() {
	testCases := []struct {
		payment ReceiptPayment
		invalid bool
	}{
		{payment: ReceiptPayment{Amount: NewMoney(1234, -2), PaymentType: CardPayment}},
		{payment: ReceiptPayment{Amount: NewMoney(1234, -2), PaymentType: FiscalPaymentType("bad_type")}, invalid: true},
		{payment: ReceiptPayment{Amount: NewMoney(1234, -2)}, invalid: true},
		{payment: ReceiptPayment{PaymentType: CardPayment}, invalid: true},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprint(i), func() {
			err := tc.payment.Validate()

			if tc.invalid {
				s.Assert().Error(err)
			} else {
				s.Assert().NoError(err)
			}
		})
	}
}

func (s *ReceiptTestSuite) TestContent() {

	testCases := []struct {
		contentChanger func(*Content)
		valid          bool
	}{
		{valid: true},
		{contentChanger: func(content *Content) { content.FirmInn = "" }},
		{contentChanger: func(content *Content) { content.FirmInn = "abc" }},
		{contentChanger: func(content *Content) { content.ReceiptType = "bad_type" }},
		{contentChanger: func(content *Content) { content.ReceiptType = "" }},
		{contentChanger: func(content *Content) { content.TaxationType = "bad_type" }},
		{contentChanger: func(content *Content) { content.TaxationType = "" }},
		{contentChanger: func(content *Content) { content.ClientEmailOrPhone = "" }},
		{contentChanger: func(content *Content) { content.FirmURL = "" }},
		{contentChanger: func(content *Content) { content.FirmReplyEmail = "" }},
		{contentChanger: func(content *Content) { content.FirmReplyEmail = "" }},
		{contentChanger: func(content *Content) { content.CompositeEventID = CompositeEventID{} }},
		{contentChanger: func(content *Content) { content.Rows = []ReceiptRow{} }},
		{contentChanger: func(content *Content) { content.Rows = []ReceiptRow{{}} }},
		{contentChanger: func(content *Content) { content.Payments = []ReceiptPayment{} }},
		{contentChanger: func(content *Content) { content.Payments = []ReceiptPayment{{}} }},
		{contentChanger: func(content *Content) { content.AgentType = Agent }, valid: true},
		{contentChanger: func(content *Content) { content.AgentType = "bad_agent" }},
		{contentChanger: func(content *Content) { content.TaxCalcMethod = CalcTaxByTotal }, valid: true},
		{contentChanger: func(content *Content) { content.TaxCalcMethod = "bad_method" }},
		{contentChanger: func(content *Content) { content.FiscalDocType = OrdinaryReceipt }, valid: true},
		{contentChanger: func(content *Content) { content.FiscalDocType = "bad_doc_type" }},
		{contentChanger: func(content *Content) { content.SupplierPhone = "12345" }, valid: true},
		{contentChanger: func(content *Content) { content.SupplierPhone = "12" }},
		{contentChanger: func(content *Content) { content.Payments[0].Amount = NewMoney(1500, -2) }},
		{contentChanger: func(content *Content) { content.Rows[0].Price = NewMoney(1500, -2) }},
		{contentChanger: func(content *Content) { content.Rows[0].Qty = NewQuantity(2, 0) }},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprint(i), func() {
			validCompositeID := CompositeEventID{PaymentIDType: TrustPurchaseToken, PaymentID: "a1", EventID: "b2"}
			validRow := ReceiptRow{Qty: NewQuantity(1, 0), Price: NewMoney(1504, -2), Text: "Some text", PaymentTypeType: PrepaymentTypeType, TaxType: NDS20}
			validPayment := ReceiptPayment{Amount: NewMoney(1504, -2), PaymentType: CardPayment}
			validContent := Content{
				CompositeEventID:   validCompositeID,
				Rows:               []ReceiptRow{validRow},
				Payments:           []ReceiptPayment{validPayment},
				FirmReplyEmail:     "email",
				FirmURL:            "url",
				ClientEmailOrPhone: "phone",
				TaxationType:       OSN,
				ReceiptType:        Income,
				FirmInn:            "1234567890",
			}

			content := validContent
			if tc.contentChanger != nil {
				tc.contentChanger(&content)
			}

			err := content.Validate()

			if !tc.valid {
				s.Assert().Error(err)
			} else {
				s.Assert().NoError(err)
			}
		})
	}
}
