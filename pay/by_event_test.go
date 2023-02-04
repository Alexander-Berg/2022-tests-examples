package receipt

import (
	"encoding/json"
	"fmt"
	"testing"

	"github.com/google/go-cmp/cmp"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/payplatform/fes/core/schemas"
)

type EventTestSuite struct {
	suite.Suite
}

func TestEventTestSuite(t *testing.T) {
	suite.Run(t, new(EventTestSuite))
}

func (s *EventTestSuite) TestFiscalPaymentType() {
	testCases := []struct {
		paymentMethod schemas.PaymentMethod
		isDelivery    bool
		paymentType   schemas.FiscalPaymentType
	}{
		{paymentMethod: "credit::cession", paymentType: schemas.CreditPayment},
		{paymentMethod: "something_else", isDelivery: true, paymentType: schemas.Prepayment},
		{paymentMethod: "taxi_wallet_debit", paymentType: schemas.Prepayment},
		{paymentMethod: "market_certificate", paymentType: schemas.Prepayment},
		{paymentMethod: "something_else", paymentType: schemas.CardPayment},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.paymentMethod), func() {
			var eventType schemas.EventType
			if tc.isDelivery {
				eventType = schemas.Delivery
			} else {
				eventType = schemas.Hold
			}
			paymentMethod := tc.paymentMethod

			fiscalPaymentType, err := fiscalPaymentType(schemas.NonCash, paymentMethod, eventType)
			s.Require().NoError(err)
			s.Assert().Equal(fiscalPaymentType, tc.paymentType)
		})
	}
}

func (s *EventTestSuite) TestPaymentTypeType() {
	testCases := []struct {
		paymentMethod   schemas.PaymentMethod
		eventType       schemas.EventType
		expectsDelivery bool
		paymentTypeType schemas.PaymentTypeType
	}{
		{paymentMethod: "anything", eventType: schemas.Refund, paymentTypeType: schemas.PrepaymentTypeType},         // TODO Так сейчас в проде. Нужно будет сделать нормально.
		{paymentMethod: "afisha_fake_refund", eventType: schemas.Hold, paymentTypeType: schemas.PrepaymentTypeType}, // TODO Так сейчас в проде. Нужно будет сделать нормально.
		{paymentMethod: "credit::cession", eventType: schemas.Hold, paymentTypeType: schemas.CreditWDelivery},       // Переуступка кредита
		{paymentMethod: "market_sbol_cert", eventType: schemas.Hold, paymentTypeType: schemas.PrepaymentTypeType},   // Покупка сертификата маркета
		{paymentMethod: "anything", eventType: schemas.Delivery, paymentTypeType: schemas.FullPaymentWDelivery},
		{paymentMethod: "anything", eventType: schemas.Hold, expectsDelivery: true, paymentTypeType: schemas.FullPrepaymentWoDelivery},
		{paymentMethod: "anything", eventType: schemas.Hold, paymentTypeType: schemas.FullPaymentWDelivery},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %+v", i, tc), func() {
			paymentMethod := tc.paymentMethod

			fiscalPaymentType := paymentTypeType(paymentMethod, tc.eventType, tc.expectsDelivery)
			s.Assert().Equal(fiscalPaymentType, tc.paymentTypeType)
		})
	}
}

func defaultEvent() schemas.Event {
	return schemas.Event{
		EventType: schemas.Hold,
		Payment: schemas.Payment{
			Rows: []schemas.Row{{
				Price:   schemas.NewMoney(10, 0),
				Qty:     schemas.NewQuantity(10, 0),
				Title:   "Some text",
				TaxType: schemas.NDS20,
			}},
			TaxationType:      "OSN",
			PaymentMethodType: schemas.NonCash,
		},
		ServiceID:          604,
		Firm:               schemas.Firm{INN: "7736207543", RegionID: 225},
		User:               schemas.User{EmailOrPhone: "user_email"},
		CashregisterParams: schemas.CashregisterParams{},
	}
}

func defaultReceipt() schemas.Receipt {
	return schemas.Receipt{
		ReceiptContent: schemas.Content{
			FirmInn:            "7736207543",
			ReceiptType:        schemas.Income,
			TaxationType:       schemas.OSN,
			AgentType:          schemas.NoneAgent,
			ClientEmailOrPhone: "user_email",
			FirmReplyEmail:     "drive@support.yandex.ru",
			FirmURL:            "https://yandex.ru/drive/",
			CompositeEventID: schemas.CompositeEventID{
				PaymentIDType: schemas.TrustPurchaseToken,
				PaymentID:     "pid",
				EventID:       "eid",
			},
			Rows: []schemas.ReceiptRow{{
				Price:           schemas.NewMoney(10, 0),
				Qty:             schemas.NewQuantity(10, 0),
				TaxType:         schemas.NDS20,
				PaymentTypeType: schemas.FullPaymentWDelivery,
				Text:            "Some text",
			}},
			Payments: []schemas.ReceiptPayment{{Amount: schemas.NewMoney(100, 0), PaymentType: schemas.CardPayment}},
		},
	}
}

func (s *EventTestSuite) TestMakeReceipt() {
	testCases := []struct {
		name           string
		eventChanger   func(e *schemas.Event)
		invalid        bool
		receiptChanger func(r *schemas.Receipt)
	}{
		{name: "default"},
		{name: "empty taxationType", eventChanger: func(e *schemas.Event) { e.Payment.TaxationType = "" }},
		{name: "empty clientEmailOrPhone", eventChanger: func(e *schemas.Event) { e.User.EmailOrPhone = "" }, invalid: true},
		{name: "invdalid taxationType", eventChanger: func(e *schemas.Event) { e.Payment.TaxationType = "Other" }, invalid: true},
		{name: "unknown service", eventChanger: func(e *schemas.Event) { e.ServiceID = 0 }, invalid: true},
		{
			name:         "refund",
			eventChanger: func(e *schemas.Event) { e.EventType = schemas.Refund },
			receiptChanger: func(r *schemas.Receipt) {
				r.ReceiptContent.ReceiptType = schemas.ReturnIncome
				r.ReceiptContent.Rows[0].PaymentTypeType = schemas.PrepaymentTypeType
				r.ReceiptContent.Payments[0].PaymentType = schemas.CardPayment
			},
		},
		{
			name: "fake refund",
			eventChanger: func(e *schemas.Event) {
				e.Payment.TrustSpecific.PaymentMethod = "afisha_fake_refund"
			},
			receiptChanger: func(r *schemas.Receipt) {
				r.ReceiptContent.ReceiptType = schemas.ReturnIncome
				r.ReceiptContent.Rows[0].PaymentTypeType = schemas.PrepaymentTypeType
				r.ReceiptContent.Payments[0].PaymentType = schemas.CardPayment
			},
		},
		{
			name:           "agent",
			eventChanger:   func(e *schemas.Event) { e.Payment.AgentType = schemas.Agent },
			receiptChanger: func(r *schemas.Receipt) { r.ReceiptContent.AgentType = schemas.Agent },
		},
		{
			name:           "supplier phone",
			eventChanger:   func(e *schemas.Event) { e.Payment.SupplierPhone = "12345" },
			receiptChanger: func(r *schemas.Receipt) { r.ReceiptContent.SupplierPhone = "12345" },
		},
		{
			name:           "fiscal doc type",
			eventChanger:   func(e *schemas.Event) { e.Payment.FiscalDocType = schemas.BSODocument },
			receiptChanger: func(r *schemas.Receipt) { r.ReceiptContent.FiscalDocType = schemas.BSODocument },
		},
		{
			name:           "footer",
			eventChanger:   func(e *schemas.Event) { e.CashregisterParams.ReceiptFooter = "some text" },
			receiptChanger: func(r *schemas.Receipt) { r.ReceiptFooter = "some text" },
		},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.name), func() {
			event := defaultEvent()
			if tc.eventChanger != nil {
				tc.eventChanger(&event)
			}
			expectedReceipt := defaultReceipt()
			if tc.receiptChanger != nil {
				tc.receiptChanger(&expectedReceipt)
			}

			realReceipt, err := ByEvent(event, "trust", "pid", "eid")
			if tc.invalid {
				s.Require().Error(err)
			} else {
				s.Require().NoError(err)
				s.Assert().Empty(cmp.Diff(*realReceipt, expectedReceipt))
			}
		})
	}
}

func simpleEvent() schemas.Event {
	return schemas.Event{
		Payment: schemas.Payment{
			PaymentMethodType: schemas.NonCash,
			Rows: []schemas.Row{
				{Qty: schemas.NewQuantity(25, -1), Price: schemas.NewMoney(14, -1)},
			},
		},
		ServiceID: 604,
	}
}

func twoCardRowsEvent() schemas.Event {
	return schemas.Event{
		Payment: schemas.Payment{
			PaymentMethodType: schemas.NonCash,
			Rows: []schemas.Row{
				{Qty: schemas.NewQuantity(25, -1), Price: schemas.NewMoney(14, -1)},
				{Qty: schemas.NewQuantity(15, -1), Price: schemas.NewMoney(34, -1)},
			},
		},
		ServiceID: 604,
	}
}

func overriddenRowsEvent() schemas.Event {
	return schemas.Event{
		Payment: schemas.Payment{
			PaymentMethodType: schemas.NonCash,
			Rows: []schemas.Row{
				{Qty: schemas.NewQuantity(25, -1), TaxType: schemas.NDS20, Price: schemas.NewMoney(14, -1)},
				{Qty: schemas.NewQuantity(25, -1), TaxType: schemas.NDS20, Price: schemas.NewMoney(34, -1)},
			},
		},
		CashregisterParams: schemas.CashregisterParams{
			ReceiptContent: schemas.CashRegisterParamsContent{
				Rows: []schemas.ReceiptRow{
					{Qty: schemas.NewQuantity(25, -1), TaxType: schemas.NDS20, Price: schemas.NewMoney(48, -1), PaymentTypeType: "full_payment_w_delivery"},
				},
			},
		},
		ServiceID: 604,
	}
}

func compositeEvent() schemas.Event {
	return schemas.Event{
		Payment: schemas.Payment{
			Rows: []schemas.Row{
				{
					Qty: schemas.NewQuantity(1, 0), Price: schemas.NewMoney(82, -1),
					Markup: schemas.PaymentMethodMarkup{
						schemas.NonCash: schemas.NewMoney(48, -1),
						schemas.Virtual: schemas.NewMoney(34, -1),
					},
				},
			},
		},
		ServiceID: 604,
	}
}

func (s *EventTestSuite) TestPayments() {
	testCases := []struct {
		Name         string
		PaymentEvent schemas.Event
		Payments     []schemas.ReceiptPayment
	}{
		{
			Name: "Simple schemas", PaymentEvent: simpleEvent(),
			Payments: []schemas.ReceiptPayment{{PaymentType: "card", Amount: schemas.NewMoney(35, -1)}},
		},
		{
			Name: "Two card rows schemas", PaymentEvent: twoCardRowsEvent(),
			Payments: []schemas.ReceiptPayment{{PaymentType: "card", Amount: schemas.NewMoney(86, -1)}},
		},
		{
			Name: "Composite schemas", PaymentEvent: compositeEvent(),
			Payments: []schemas.ReceiptPayment{{PaymentType: "card", Amount: schemas.NewMoney(48, -1)}},
		},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.Name), func() {
			realPayments, err := receiptPayments(tc.PaymentEvent)
			s.Assert().NoError(err)
			s.Assert().Empty(cmp.Diff(realPayments, tc.Payments))
		})
	}
}

func (s *EventTestSuite) TestRows() {
	testCases := []struct {
		Name         string
		PaymentEvent schemas.Event
		Rows         []schemas.ReceiptRow
	}{
		{
			Name: "Simple schemas", PaymentEvent: simpleEvent(),
			Rows: []schemas.ReceiptRow{
				{Price: schemas.NewMoney(14, -1), Qty: schemas.NewQuantity(25, -1), PaymentTypeType: "full_payment_w_delivery"},
			},
		},
		{
			Name: "Two card rows schemas", PaymentEvent: twoCardRowsEvent(),
			Rows: []schemas.ReceiptRow{
				{Qty: schemas.NewQuantity(25, -1), Price: schemas.NewMoney(14, -1), PaymentTypeType: "full_payment_w_delivery"},
				{Qty: schemas.NewQuantity(15, -1), Price: schemas.NewMoney(34, -1), PaymentTypeType: "full_payment_w_delivery"},
			},
		},
		{
			Name: "Overridden rows schemas", PaymentEvent: overriddenRowsEvent(),
			Rows: overriddenRowsEvent().CashregisterParams.ReceiptContent.Rows,
		},
		{
			Name: "Composite schemas", PaymentEvent: compositeEvent(),
			Rows: []schemas.ReceiptRow{
				{Qty: schemas.NewQuantity(1, 0), Price: schemas.NewMoney(48, -1), PaymentTypeType: "full_payment_w_delivery"},
			},
		},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.Name), func() {
			realRows, err := receiptRows(tc.PaymentEvent)
			s.Assert().NoError(err)
			s.Assert().Empty(cmp.Diff(realRows, tc.Rows))
		})
	}
}

func (s *EventTestSuite) TestBadOverrideRows() {
	e := overriddenRowsEvent()
	e.CashregisterParams.ReceiptContent.Rows = []schemas.ReceiptRow{
		{Qty: schemas.NewQuantity(25, -1), TaxType: schemas.NDS20, Price: schemas.NewMoney(45, -1), PaymentTypeType: "full_payment_w_delivery"},
	}

	_, err := receiptRows(e)
	s.Assert().Error(err)
}

func eventStringToReceipt(eventString, paymentID, eventID string) (*schemas.Receipt, error) {
	var parsed schemas.Event
	err := json.Unmarshal([]byte(eventString), &parsed)
	if err != nil {
		return nil, err
	}
	return ByEvent(parsed, schemas.Trust, paymentID, eventID)
}

func (s *EventTestSuite) TestMakeReceipts() {
	testCases := []struct {
		name    string
		event   string
		receipt string
	}{
		{
			name:  "Drive. Hold",
			event: `{"version":1,"event_type":"Refund","payment":{"currency":"RUB","rows":[{"price":"271.82","qty":"0","tax_type":"nds_20","title":"Арендная плата за аренду транспортного средства","supplier_info":{"inn":"","phone":"","name":""}}],"agent_type":"none_agent","payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":604,"trust_specific": {"terminal_id":57000653},"firm":{"inn": "7704448440", "region_id": 225},"user":{"email_or_phone":"kapral382@gmail.com","region_id":225}}`,
			receipt: `{"receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "22cf07ab232f945537d2fd9ddd067e3f", "event_id": "Refund_621310b23b31769a77b739f4"}, "agent_type": "none_agent", "client_email_or_phone": "kapral382@gmail.com", "firm_inn": "7704448440", "firm_reply_email": "drive@support.yandex.ru", "firm_url": "https://yandex.ru/drive/", "payments": [{"amount": "271.82", "payment_type": "card"}], "receipt_type": "return_income", "rows": [{"payment_type_type": "prepayment", 			  "price": "271.82", "qty": "1.000", "tax_type": "nds_20", "text": "Арендная плата за аренду транспортного средства"}], "taxation_type": "OSN"}}`,
		},
		{
			name:  "Drive. Refund",
			event: `{"version":1,"event_type":"Hold","payment":{"currency":"RUB","rows":[{"price":"271.82","qty":"0","tax_type":"nds_20","title":"Арендная плата за аренду транспортного средства"}],"agent_type":"none_agent","payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":604,"trust_specific": {"terminal_id":57000653},"firm":{"inn": "7704448440", "region_id": 225},"user":{"email_or_phone":"kapral382@gmail.com","region_id":225}}`,
			receipt: `{"receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "22cf07ab232f945537d2fd9ddd067e3f", "event_id": "Hold_22cf07ab232f945537d2fd9ddd067e3f"}, "agent_type": "none_agent", "client_email_or_phone": "kapral382@gmail.com", "firm_inn": "7704448440", "firm_reply_email": "drive@support.yandex.ru", "firm_url": "https://yandex.ru/drive/", "payments": [{"amount": "271.82", "payment_type": "card"}], "receipt_type": "income", 		"rows": [{"payment_type_type": "full_payment_w_delivery", "price": "271.82", "qty": "1.000", "tax_type": "nds_20", "text": "Арендная плата за аренду транспортного средства"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Taxi. Not caneceled",
			event:   `{"version":1,"event_type":"Hold","payment":{"currency":"RUB","rows":[{"price":"132","qty":"0","tax_type":"nds_none","title":"Перевозка пассажиров и багажа","supplier_info":{"inn":"572008265297"}}],"payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false,"fiscal_type":""}},"service_id":124,"trust_specific": {"terminal_id":57000589},"firm":{"inn": "7704340310", "region_id": 225},"user":{"email_or_phone":"+79892129589","region_id":225}}`,
			receipt: `{"receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "d7602f02439a37b025236c0d1a7c3946", "event_id": "Hold_d7602f02439a37b025236c0d1a7c3946"}, "agent_type": "agent", "client_email_or_phone": "+79892129589", "firm_inn": "7704340310", "firm_reply_email": "support@go.yandex.com", "firm_url": "taxi.yandex.ru", "payments": [{"amount": "132.00", "payment_type": "card"}], "receipt_type": "income", "rows": [{"payment_type_type": "full_payment_w_delivery", "price": "132.00", "qty": "1.000", "supplier_info": {"inn": "572008265297"}, "tax_type": "nds_none", "text": "Перевозка пассажиров и багажа"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Music. Composite payment",
			event:   `{"version":1,"event_type":"Hold","payment":{"currency":"RUB","rows":[{"price":"299","qty":"1","tax_type":"nds_20","title":"подписка Плюс Мульти","markup":{"non-cash":"219","virtual":"80"}}],"agent_type":"none_agent","payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":711,"trust_specific": {"terminal_id":57000237},"firm":{"inn": "7736207543", "region_id": 225},"user":{"email_or_phone":"kinzikiewa92.07@gmail.com","region_id":225}}`,
			receipt: `{"receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "ef5270e3122f6cd7f718c995c8e134a7", "event_id": "Hold_ef5270e3122f6cd7f718c995c8e134a7"}, "agent_type": "none_agent", "client_email_or_phone": "kinzikiewa92.07@gmail.com", "firm_inn": "7736207543", "firm_reply_email": "music@support.yandex.ru", "firm_url": "yandex.ru", "payments": [{"amount": "219.00", "payment_type": "card"}], "receipt_type": "income", "rows": [{"payment_type_type": "full_payment_w_delivery", "price": "219.00", "qty": "1.000", "tax_type": "nds_20", "text": "подписка Плюс Мульти"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Taxi. Hold",
			event:   `{"version":1,"event_type":"Hold","payment":{"currency":"RUB","rows":[{"price":"114","qty":"0","tax_type":"nds_none","title":"Перевозка пассажиров и багажа","supplier_info":{"inn":"711703111760"}}],"payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":124,"trust_specific": {"terminal_id":660402044},"firm":{"inn": "7704340310", "region_id": 225},"user":{"email_or_phone":"+79963379029","region_id":225}}`,
			receipt: `{"receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "ec19fb0ac38789b0e323431f259099f3", "event_id": "Hold_ec19fb0ac38789b0e323431f259099f3"}, "agent_type": "agent", "client_email_or_phone": "+79963379029", "firm_inn": "7704340310", "firm_reply_email": "support@go.yandex.com", "firm_url": "taxi.yandex.ru", "payments": [{"amount": "114.00", "payment_type": "card"}], "receipt_type": "income", "rows": [{"payment_type_type": "full_payment_w_delivery", "price": "114.00", "qty": "1.000", "supplier_info": {"inn": "711703111760"}, "tax_type": "nds_none", "text": "Перевозка пассажиров и багажа"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Taxi. Partial refund",
			event:   `{"version":1,"event_type":"Refund","payment":{"currency":"RUB","rows":[{"price":"114","qty":"0","tax_type":"nds_none","title":"Перевозка пассажиров и багажа","supplier_info":{"inn":"711703111760"}}],"payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":124,"trust_specific": {"terminal_id":660402044},"firm":{"inn": "7704340310", "region_id": 225},"user":{"email_or_phone":"+79963379029","region_id":225}}`,
			receipt: `{"cashregister_params": {}, "receipt_content": {"composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "ec19fb0ac38789b0e323431f259099f3", "event_id": "Refund_6213112232da8358e34818c7"}, "agent_type": "agent", "client_email_or_phone": "+79963379029", "firm_inn": "7704340310", "firm_reply_email": "support@go.yandex.com", "firm_url": "taxi.yandex.ru", "payments": [{"amount": "114.00", "payment_type": "card"}], "receipt_type": "return_income", "rows": [{"payment_type_type": "prepayment", "price": "114.00", "qty": "1.000", "supplier_info": {"inn": "711703111760"}, "tax_type": "nds_none", "text": "Перевозка пассажиров и багажа"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Fuel. Hold with cashregister_params",
			event:   `{"version":1,"event_type":"Hold","payment":{"currency":"RUB","rows":[{"price":"11.84","qty":"1","tax_type":"nds_20_120","title":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)"},{"price":"45.17","qty":"10.21","tax_type":"nds_20_120","title":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)"}],"agent_type":"none_agent","payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":636,"trust_specific": {"terminal_id":56124001},"firm":{"inn": "7704460769", "region_id": 225},"user":{"email_or_phone":"taksopark1-yola11814628@yandex.ru","region_id":225},"cashregister_params":{"receipt_content":{"rows":[{"price":"46.33","qty":"10.21","tax_type":"nds_20_120","payment_type_type":"full_prepayment_wo_delivery","text":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)","agent_type":"none_agent"}],"tax_calc_method":"total"}}}`,
			receipt: `{"receipt_content": {"tax_calc_method": "total", "composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "465fecc872e162eab77069998262afe0", "event_id": "Hold_465fecc872e162eab77069998262afe0"}, "agent_type": "none_agent", "client_email_or_phone": "taksopark1-yola11814628@yandex.ru", "firm_inn": "7704460769", "firm_reply_email": "support@tanker.yandex.ru", "firm_url": "https://tanker.yandex.ru/", "payments": [{"amount": "473.03", "payment_type": "card"}], "receipt_type": "income", "rows": [{"text": "Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)", "price": "46.33", "payment_type_type": "full_prepayment_wo_delivery", "qty": "10.21", "agent_type": "none_agent", "tax_type": "nds_20_120"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Fuel. Delivery with cashregister_params",
			event:   `{"version":1,"event_type":"Delivery","payment":{"currency":"RUB","rows":[{"price":"11.84","qty":"1","tax_type":"nds_20","title":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)"},{"price":"45.17","qty":"10.21","tax_type":"nds_20","agent_type":"","title":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)"}],"agent_type":"none_agent","payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false}},"service_id":636,"trust_specific": {"terminal_id":56124001},"firm":{"inn": "7704460769", "region_id": 225},"user":{"email_or_phone":"taksopark1-yola11814628@yandex.ru","region_id":225},"cashregister_params":{"receipt_content":{"rows":[{"price":"46.33","qty":"10.21","tax_type":"nds_20","payment_type_type":"full_payment_w_delivery","text":"Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)","agent_type":"none_agent"}],"tax_calc_method":"total"}}}`,
			receipt: `{"receipt_content": {"tax_calc_method": "total", "composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "465fecc872e162eab77069998262afe0", "event_id": "Delivery_465fecc872e162eab77069998262afe0"}, "agent_type": "none_agent", "client_email_or_phone": "taksopark1-yola11814628@yandex.ru", "firm_inn": "7704460769", "firm_reply_email": "support@tanker.yandex.ru", "firm_url": "https://tanker.yandex.ru/", "payments": [{"amount": "473.03", "payment_type": "prepayment"}], "receipt_type": "income", "rows": [{"text": "Оплата топлива АИ-92 Танеко, Татнефть №493 (Марийский ф-л)", "price": "46.33", "payment_type_type": "full_payment_w_delivery", "qty": "10.21", "agent_type": "none_agent", "tax_type": "nds_20"}], "taxation_type": "OSN"}}`,
		},
		{
			name:    "Afisha fake refund",
			event:   `{"version":1,"event_type":"Hold","payment":{"currency":"RUR","rows":[{"price":"240","qty":"0","tax_type":"nds_20","title":"Билет на концерт: Танцевальный партер, сервисный сбор","supplier_info":{"inn":"9705121040"}},{"price":"2400","qty":"0","tax_type":"nds_none","title":"Билет на концерт: Танцевальный партер","supplier_info":{"inn":"7743774790"}}],"payment_method_type":"non-cash","is_auto_refund":false,"trust_specific":{"is_external_binding":false,"expects_delivery":false,"fiscal_type":"","payment_method":"afisha_fake_refund"}},"service_id":131,"trust_specific": {"terminal_id":57000140},"firm":{"inn": "9705121040", "region_id": 225},"user":{"email_or_phone":"ella.74.93@yandex.ru","region_id":225}}`,
			receipt: `{"fp": 3170287412, "ofd_ticket_received": false, "localzone": "Europe/Moscow", "check_url": "https://check.yandex.ru/?n=118073&fn=9289000100609827&fpd=3170287412", "ofd": {"inn": "7704358518", "check_url": "nalog.ru", "name": "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ОФД\""}, "shift_number": 861, "kkt": {"automatic_machine_number": "101922631", "rn": "0003086692023135", "version": "3.5.30", "sn": "00000003820081980716"}, "receipt_content": {"rows": [{"supplier_info": {"inn": "9705121040"}, "text": "Билет на концерт: Танцевальный партер, сервисный сбор", "price": "240.00", "payment_type_type": "prepayment", "qty": "1.000", "tax_type": "nds_20"}, {"supplier_info": {"inn": "7743774790"}, "text": "Билет на концерт: Танцевальный партер", "price": "2400.00", "payment_type_type": "prepayment", "qty": "1.000", "tax_type": "nds_none"}], "firm_reply_email": "tickets@support.yandex.ru", "firm_url": "http://afisha.yandex.ru", "firm_inn": "9705121040", "agent_type": "agent", "payments": [{"amount": "2640.00", "payment_type": "card"}], "client_email_or_phone": "ella.74.93@yandex.ru", "composite_event_id": {"payment_id_type": "trust_purchase_token", "payment_id": "9591a59553fd959d20902e96c787d835", "event_id": "Hold_9591a59553fd959d20902e96c787d835"}, "taxation_type": "OSN", "receipt_type": "return_income"}, "firm": {"inn": "9705121040", "reply_email": "tickets@support.yandex.ru", "name": "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.МЕДИАСЕРВИСЫ\""}, "origin": "online", "location": {"description": "http://afisha.yandex.ru", "address": "141004, Россия, Московская обл., г. Мытищи, ул. Силикатная, д. 19"}, "receipt_calculated_content": {"tax_totals": [{"tax_pct": "20.00", "tax_type": "nds_20", "tax_amount": "40.00"}, {"tax_pct": "0.00", "tax_type": "nds_none", "tax_amount": "0.00"}], "rows": [{"tax_pct": "20.00", "supplier_info": {"inn": "9705121040"}, "tax_amount": "40.00", "text": "Билет на концерт: Танцевальный партер, сервисный сбор", "price": "240.00", "payment_type_type": "prepayment", "qty": "1.000", "amount": "240.00", "tax_type": "nds_20"}, {"tax_pct": "0.00", "supplier_info": {"inn": "7743774790"}, "tax_amount": "0.00", "text": "Билет на концерт: Танцевальный партер", "price": "2400.00", "payment_type_type": "prepayment", "qty": "1.000", "amount": "2400.00", "tax_type": "nds_none"}], "firm_reply_email": "tickets@support.yandex.ru", "firm_url": "http://afisha.yandex.ru", "totals": [{"amount": "2640.00", "payment_type": "card"}], "qr": "t=20210316T0805&s=2640.00&fn=9289000100609827&i=118073&fp=3170287412&n=2", "total": "2640.00", "money_received_total": "2640.00"}, "dt": "2021-03-16 08:05:00", "document_index": 67, "id": 118073, "fn": {"model": "ФН-1", "sn": "9289000100609827"}}`,
		},
	}

	for i, tc := range testCases {
		s.Run(fmt.Sprintf("%d. %s", i, tc.name), func() {
			testCase(s, tc.event, tc.receipt)
		})
	}

}

func testCase(s *EventTestSuite, e string, receiptString string) {
	var expectedReceipt schemas.Receipt
	err := json.Unmarshal([]byte(receiptString), &expectedReceipt)
	s.Require().NoError(err)

	compositeEventID := expectedReceipt.ReceiptContent.CompositeEventID
	realReceipt, err := eventStringToReceipt(e, compositeEventID.PaymentID, compositeEventID.EventID)
	s.Require().NoError(err)

	s.Require().Empty(cmp.Diff(*realReceipt, expectedReceipt))
}
