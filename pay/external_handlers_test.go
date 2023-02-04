package server

import (
	"encoding/base64"
	"math/rand"
	"net/http"
	"net/url"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/mastercard"
	"a.yandex-team.ru/pay/duckgo/paymenttoken"
	"a.yandex-team.ru/pay/duckgo/visa"
)

func TestVisaVerifyRequest(t *testing.T) {
	type testCase struct {
		name    string
		payload string
		u       string
		method  string
	}

	testCases := []testCase{
		{"empty", "", "http://yandex.ru", http.MethodGet},
		{"get_with_query", "", "http://yandex.ru?a=b&b=c", http.MethodGet},
		{"empty_post", "", "http://yandex.ru", http.MethodPost},
		{"with_body", "{\"pay\":\"load\"}", "http://yandex.ru", http.MethodPost},
		{"with_body_and_query", "{\"pay\":\"load\"}", "http://yandex.ru?arg1=1&arg2=2", http.MethodPost},
	}

	te := NewTestEnv(t, withSameVisaVerificationAndSigningKey())
	defer te.Close()

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			getValidSignature := func() (*url.URL, string) {
				resp, err := te.Client.VisaSignRequest(te.BaseCtx, &SignRequest{
					URL:    tc.u,
					Method: tc.method,
					Body:   base64.StdEncoding.EncodeToString([]byte(tc.payload)),
				})
				require.NoError(t, err)
				signature, ok := resp.Headers[visa.AuthorizationHeaderName]
				require.True(t, ok, "auth header required")

				u, err := url.Parse(resp.URL)
				require.NoError(t, err)
				return u, signature[0]
			}

			u, validSignature := getValidSignature()

			err := te.Client.VisaVerifyRequest(te.BaseCtx, &VisaVerifyRequest{
				Signature: validSignature,
				URL:       u.String(),
				Body:      base64.StdEncoding.EncodeToString([]byte(tc.payload)),
			})
			require.NoError(t, err)
		})
	}
}

func TestVisaVerifyRequestMustFail(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	err := te.Client.VisaVerifyRequest(te.BaseCtx, &VisaVerifyRequest{
		Signature: "invalid",
		URL:       "http://ya.ru?apiKey=123",
		Body:      base64.StdEncoding.EncodeToString([]byte("data")),
	})
	require.Error(t, err)
}

func TestVisaSigning(t *testing.T) {
	type testCase struct {
		name    string
		payload string
		u       string
		method  string
	}

	testCases := []testCase{
		{"empty", "", "http://yandex.ru", http.MethodGet},
		{"get_with_query", "", "http://yandex.ru?a=b&b=c", http.MethodGet},
		{"empty_post", "", "http://yandex.ru", http.MethodPost},
		{"with_body", "{\"pay\":\"load\"}", "http://yandex.ru", http.MethodPost},
		{"with_body_and_query", "{\"pay\":\"load\"}", "http://yandex.ru?arg1=1&arg2=2", http.MethodPost},
	}
	te := NewTestEnv(t)
	defer te.Close()

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			resp, err := te.Client.VisaSignRequest(te.BaseCtx, &SignRequest{
				URL:    tc.u,
				Method: tc.method,
				Body:   base64.StdEncoding.EncodeToString([]byte(tc.payload)),
			})
			require.NoError(t, err)
			header, ok := resp.Headers[visa.AuthorizationHeaderName]
			require.True(t, ok, "auth header required")
			require.NotEqual(t, "", header, "auth header must not be empty")

			originURL, err := url.Parse(tc.u)
			require.NoError(t, err)

			newURL, err := url.Parse(resp.URL)
			require.NoError(t, err)
			newParams := newURL.Query()

			kid, hasKid := newParams[visa.AuthorizationQueryParamName]
			require.True(t, hasKid, "keyId parameter is expected")
			require.NotEmpty(t, kid, "keyId must not be empty")

			for name, value := range originURL.Query() {
				newValue, ok := newParams[name]
				require.True(t, ok, "missing parameter")
				require.Equal(t, value, newValue, "param value changed")
			}
		})

	}

}

func TestMastercardCheckoutFixYear(t *testing.T) {
	type testCase struct {
		inYear       string
		expectedYear int
	}

	testCases := []testCase{
		{"21", 2021},
		{"99", 2099},
		{"00", 2000},
		{"2021", 2021},
	}

	te := NewTestEnv(t)
	defer te.Close()

	for _, tc := range testCases {
		t.Run(tc.inYear, func(t *testing.T) {
			testMastercardCheckoutFixYear(t, te, tc.inYear, tc.expectedYear)
		})
	}
}

func genRandomPAN() string {
	l := []byte("0123456789")
	r := make([]byte, 16)
	for i := 0; i < len(r); i++ {
		r[i] = l[rand.Intn(len(l))]
	}

	return string(r)
}

func testMastercardCheckoutFixYear(t *testing.T, te *TestEnv, inYear string, expectedYear int) {
	enrollRequest := &MastercardEnrollRequest{
		Card: mastercard.Card{
			PrimaryAccountNumber: genRandomPAN(),
			PanExpirationMonth:   "01",
			PanExpirationYear:    inYear,
			CardholderFullName:   "John Donne",
			CardSecurityCode:     "123",
		},
		CardSource: mastercard.CardSourceMerchant,
		AccountID:  "uid-hash",
	}

	enrollment, err := te.Client.MastercardEnroll(te.BaseCtx, enrollRequest)
	require.NoError(t, err)

	gwKey := generateECP256Key(t)
	gatewayID := "gateway-id"

	checkoutRequest := &MastercardCheckoutRequest{
		CardID: enrollment.MaskedCard.SRCDigitalCardID,
		CheckoutRequestBase: CheckoutRequestBase{
			RecipientID:                 gatewayID,
			GatewayMerchantID:           "gateway-merchant-id",
			RecipientPublicKey:          convertPublicKeyToDERBase64(t, gwKey.Public()),
			RecipientPublicKeySignature: te.SignRecipientKey(&gwKey.PublicKey),
			TransactionInfo: TransactionInfo{
				Currency: "RUB",
				Amount:   100,
			},
			MessageID:         genUUID(),
			MessageExpiration: time.Now().Unix() * 1000,
		},
	}
	checkoutResponse, err := te.Client.MastercardCheckout(te.BaseCtx, checkoutRequest)
	require.NoError(t, err)

	recipient := paymenttoken.NewRecipient(gatewayID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(gwKey))
	token := &paymenttoken.EncryptedMessage{}
	err = recipient.UnsealJSONMessage(checkoutResponse.PaymentToken, token)
	require.NoError(t, err)

	assert.Equal(t, expectedYear, token.PaymentMethodDetails.ExpirationYear)
}

type checkoutTransactionInfoTestCase struct {
	name            string
	transactionInfo TransactionInfo
	expectedError   bool
}

func TestCheckoutTransactionInfo(t *testing.T) {
	testCases := []checkoutTransactionInfoTestCase{
		{
			name:            "empty",
			transactionInfo: TransactionInfo{},
			expectedError:   true,
		},
		{
			name: "zero_amount",
			transactionInfo: TransactionInfo{
				Currency: "RUB",
				Amount:   0,
			},
		},
		{
			name: "negative_amount",
			transactionInfo: TransactionInfo{
				Currency: "RUB",
				Amount:   -1,
			},
			expectedError: true,
		},
		{
			name: "empty_currency",
			transactionInfo: TransactionInfo{
				Currency: "",
				Amount:   10,
			},
			expectedError: true,
		},
	}

	te := NewTestEnv(t)
	defer te.Close()

	card := PANCheckoutCard{
		PrimaryAccountNumber: "1111111111111111",
		PanExpirationMonth:   01,
		PanExpirationYear:    2020,
	}

	mcEnrollRequest := &MastercardEnrollRequest{
		Card: mastercard.Card{
			PrimaryAccountNumber: "2222222222222222",
			PanExpirationMonth:   "01",
			PanExpirationYear:    "2020",
			CardholderFullName:   "John Donne",
			CardSecurityCode:     "123",
		},
		CardSource: mastercard.CardSourceMerchant,
		AccountID:  "uid-hash",
	}

	mcEnrollment, err := te.Client.MastercardEnroll(te.BaseCtx, mcEnrollRequest)
	require.NoError(t, err)

	visaEnrollment, err := te.Client.VisaEnroll(te.BaseCtx, &VisaEnrollRequest{
		Card: visa.Card{
			PrimaryAccountNumber: "2222222222222222",
			PanExpirationMonth:   "01",
			PanExpirationYear:    "2020",
		},
		EmailHash:      "61b177aa2ec5022af4bbe431581e7f79a1d361c1432ed646cb9236e31651a162",
		Locale:         "ru_RU",
		PANSource:      visa.PANSourceOnfile,
		AccountID:      "uid-hash",
		RelationshipID: te.GetVisaAllowedRelationshipID(),
	})
	require.NoError(t, err)

	psp := newTestPaymentGateway(te)

	for _, tc := range testCases {
		t.Run(tc.name+"_mc", func(t *testing.T) {
			testMastercardCheckoutTransactionInfo(t, te, psp, mcEnrollment.MaskedCard.SRCDigitalCardID, &tc)
		})
		t.Run(tc.name+"_visa", func(t *testing.T) {
			testVisaCheckoutTransactionInfo(t, te, psp, visaEnrollment.ProvisionedTokenID, &tc)
		})
		t.Run(tc.name+"_pan", func(t *testing.T) {
			testPANCheckoutTransactionInfo(t, te, psp, &card, &tc)
		})
	}
}

func checkTransactionInfo(te *TestEnv, psp *testPSP, serializedToken string, tc *checkoutTransactionInfoTestCase) {
	t := te.t
	token := &paymenttoken.EncryptedMessage{}
	psp.MustDecryptToken(serializedToken, token)

	assert.Equal(t, tc.transactionInfo.Amount, token.TransactionDetails.Amount)
	assert.Equal(t, tc.transactionInfo.Currency, token.TransactionDetails.Currency)
}

func testVisaCheckoutTransactionInfo(t *testing.T, te *TestEnv, psp *testPSP, provisionedTokenID string, tc *checkoutTransactionInfoTestCase) {
	checkoutRequest := &VisaCheckoutRequest{
		ProvisionedTokenID:  provisionedTokenID,
		ClientPaymentDataID: "iddqd",
		CheckoutRequestBase: CheckoutRequestBase{
			RecipientID:                 psp.ID,
			GatewayMerchantID:           "gateway-merchant-id",
			RecipientPublicKey:          psp.GetSerializedPublicKey(),
			RecipientPublicKeySignature: psp.GetKeySignature(),
			TransactionInfo:             tc.transactionInfo,
			MITInfo: &MITInfo{
				Recurring: true,
				Deferred:  true,
			},
			MessageExpiration: time.Now().Unix() * 1000,
			MessageID:         genUUID(),
		},
		RelationshipID: te.GetVisaAllowedRelationshipID(),
	}

	resp, err := te.Client.VisaCheckout(te.BaseCtx, checkoutRequest)
	if tc.expectedError {
		assert.Error(t, err)
		return
	}
	require.NoError(t, err)
	checkTransactionInfo(te, psp, resp.PaymentToken, tc)
}

func testMastercardCheckoutTransactionInfo(t *testing.T, te *TestEnv, psp *testPSP, cardID string, tc *checkoutTransactionInfoTestCase) {
	checkoutRequest := &MastercardCheckoutRequest{
		CardID: cardID,
		CheckoutRequestBase: CheckoutRequestBase{
			RecipientID:                 psp.ID,
			GatewayMerchantID:           "gateway-merchant-id",
			RecipientPublicKey:          psp.GetSerializedPublicKey(),
			RecipientPublicKeySignature: psp.GetKeySignature(),
			TransactionInfo:             tc.transactionInfo,
			MITInfo: &MITInfo{
				Recurring: true,
				Deferred:  true,
			},
			MessageExpiration: time.Now().Unix() * 1000,
			MessageID:         genUUID(),
		},
	}

	resp, err := te.Client.MastercardCheckout(te.BaseCtx, checkoutRequest)
	if tc.expectedError {
		assert.Error(t, err)
		return
	}
	require.NoError(t, err)
	checkTransactionInfo(te, psp, resp.PaymentToken, tc)
}

func testPANCheckoutTransactionInfo(t *testing.T, te *TestEnv, psp *testPSP, card *PANCheckoutCard, tc *checkoutTransactionInfoTestCase) {
	checkoutRequest := &PANCheckoutRequest{
		Card:                        *card,
		RecipientID:                 psp.ID,
		GatewayMerchantID:           "gateway-merchant-id",
		RecipientPublicKey:          psp.GetSerializedPublicKey(),
		RecipientPublicKeySignature: psp.GetKeySignature(),
		TransactionInfo:             tc.transactionInfo,
		MITInfo: &MITInfo{
			Recurring: true,
			Deferred:  true,
		},
		MessageID:         genUUID(),
		MessageExpiration: time.Now().Unix() * 1000,
	}

	checkoutResponse, err := te.Client.PANCheckout(te.BaseCtx, checkoutRequest)
	if tc.expectedError {
		assert.Error(t, err)
		return
	}
	require.NoError(t, err)
	checkTransactionInfo(te, psp, checkoutResponse.PaymentToken, tc)
}

func TestMastercardCheckout(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	type testCase struct {
		name        string
		req         *MastercardCheckoutRequest
		expectError bool
	}

	validExpiration := time.Now().Add(time.Hour).Unix() * 1000
	validMerchantID := genUUID()
	validMessageID := genUUID()
	psp := newTestPaymentGateway(te)
	validTransactionInfo := TransactionInfo{
		Currency: "USD",
		Amount:   499,
	}
	validMITInfo := &MITInfo{
		Recurring: true,
		Deferred:  true,
	}

	validCardExpirationYear := 2020
	validCardExpirationMonth := 1
	validCard := mastercard.Card{
		PrimaryAccountNumber: "2222222222222222",
		PanExpirationMonth:   strconv.Itoa(validCardExpirationMonth),
		PanExpirationYear:    strconv.Itoa(validCardExpirationYear),
		CardholderFullName:   "John Donne",
		CardSecurityCode:     "123",
	}

	enrollRequest := &MastercardEnrollRequest{
		Card:       validCard,
		CardSource: mastercard.CardSourceMerchant,
		AccountID:  genUUID(),
	}

	enrollment, err := te.Client.MastercardEnroll(te.BaseCtx, enrollRequest)
	require.NoError(t, err)

	validCardID := enrollment.MaskedCard.SRCDigitalCardID

	testCases := []testCase{
		{
			name: "empty_card_id",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
			},
			expectError: true,
		},
		{
			name: "legit",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
		},
		{
			name: "empty_message_id",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "empty_expiration",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "invalid_pub_key",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          "18387573732",
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "empty_pub_key",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "empty_recipient_id",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "empty_gw_merchant_id",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
		{
			name: "zero_amount_but_mit_not_allowed",
			req: &MastercardCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					GatewayMerchantID:           validMerchantID,
					TransactionInfo: TransactionInfo{
						Amount:   0,
						Currency: "RUB",
					},
					MessageExpiration: validExpiration,
					MessageID:         validMessageID,
				},
				CardID: validCardID,
			},
			expectError: true,
		},
	}

	testMastercardCheckout := func(t *testing.T, tc testCase) {
		resp, err := te.Client.MastercardCheckout(te.BaseCtx, tc.req)
		if tc.expectError {
			require.Error(t, err)
			return
		}
		require.NoError(t, err)

		recipient := paymenttoken.NewRecipient(psp.ID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(psp.Key))
		token := &paymenttoken.EncryptedMessage{}
		err = recipient.UnsealJSONMessage(resp.PaymentToken, token)
		require.NoError(t, err)

		assert.Equal(t, tc.req.MessageID, token.MessageID)
		assert.Equal(t, tc.req.GatewayMerchantID, token.GatewayMerchantID)
		assert.Equal(t, strconv.FormatInt(tc.req.MessageExpiration, 10), token.MessageExpiration)
		assert.Equal(t, 499, token.TransactionDetails.Amount)
		assert.Equal(t, "USD", token.TransactionDetails.Currency)
		assert.Equal(t, paymenttoken.PaymentMethodCard, token.PaymentMethod)
		assert.NotEmpty(t, token.PaymentAccountReference)
		assert.Equal(t, validCardExpirationYear, token.PaymentMethodDetails.ExpirationYear)
		assert.Equal(t, validCardExpirationMonth, token.PaymentMethodDetails.ExpirationMonth)
		assert.NotEmpty(t, token.PaymentMethodDetails.PAN)
		assert.Equal(t, paymenttoken.AuthMethodCloudToken, token.PaymentMethodDetails.AuthMethod)
		assert.NotEmpty(t, token.PaymentMethodDetails.Cryptogram)
		assert.Equal(t, token.MITDetails.Recurring, true)
		assert.Equal(t, token.MITDetails.Deferred, true)
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			testMastercardCheckout(t, tc)
		})
	}

	t.Run("mitInfoOmitted", func(t *testing.T) {
		req := &MastercardCheckoutRequest{
			CheckoutRequestBase: CheckoutRequestBase{
				RecipientID:                 psp.ID,
				GatewayMerchantID:           validMerchantID,
				RecipientPublicKey:          psp.GetSerializedPublicKey(),
				RecipientPublicKeySignature: psp.GetKeySignature(),
				TransactionInfo:             validTransactionInfo,
				MessageExpiration:           validExpiration,
				MessageID:                   validMessageID,
			},
			CardID: validCardID,
		}
		resp, err := te.Client.MastercardCheckout(te.BaseCtx, req)
		require.NoError(t, err)

		recipient := paymenttoken.NewRecipient(psp.ID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(psp.Key))
		token := &paymenttoken.EncryptedMessage{}
		err = recipient.UnsealJSONMessage(resp.PaymentToken, token)
		require.NoError(t, err)

		assert.Empty(t, token.MITDetails)
	})
}

func TestVisaCheckout(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	type testCase struct {
		name        string
		req         *VisaCheckoutRequest
		expectError bool
	}

	validExpiration := time.Now().Add(time.Hour).Unix() * 1000
	validMerchantID := genUUID()
	validMessageID := genUUID()
	clientPaymentDataID := genUUID()

	psp := newTestPaymentGateway(te)
	validTransactionInfo := TransactionInfo{
		Currency: "USD",
		Amount:   499,
	}
	validMITInfo := &MITInfo{
		Recurring: true,
		Deferred:  true,
	}

	validCardExpirationYear := 2020
	validCardExpirationMonth := 1
	validCard := visa.Card{
		PrimaryAccountNumber: "2222222222222222",
		PanExpirationMonth:   strconv.Itoa(validCardExpirationMonth),
		PanExpirationYear:    strconv.Itoa(validCardExpirationYear),
	}

	enrollRequest := &VisaEnrollRequest{
		Card:           validCard,
		EmailHash:      "61b177aa2ec5022af4bbe431581e7f79a1d361c1432ed646cb9236e31651a162",
		Locale:         "ru_RU",
		PANSource:      visa.PANSourceOnfile,
		AccountID:      genUUID(),
		RelationshipID: te.GetVisaAllowedRelationshipID(),
	}

	enrollment, err := te.Client.VisaEnroll(te.BaseCtx, enrollRequest)
	require.NoError(t, err)

	validCardID := enrollment.ProvisionedTokenID

	testCases := []testCase{
		{
			name: "empty_card_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "no_client_payment_data_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ProvisionedTokenID: validCardID,
				RelationshipID:     te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "legit",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
		},
		{
			name: "empty_message_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:        psp.ID,
					GatewayMerchantID:  validMerchantID,
					RecipientPublicKey: psp.GetSerializedPublicKey(),
					TransactionInfo:    validTransactionInfo,
					MITInfo:            validMITInfo,
					MessageExpiration:  validExpiration,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "empty_expiration",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:        psp.ID,
					GatewayMerchantID:  validMerchantID,
					RecipientPublicKey: psp.GetSerializedPublicKey(),
					TransactionInfo:    validTransactionInfo,
					MITInfo:            validMITInfo,
					MessageID:          validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "invalid_pub_key",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:        psp.ID,
					GatewayMerchantID:  validMerchantID,
					RecipientPublicKey: "18387573732",
					TransactionInfo:    validTransactionInfo,
					MITInfo:            validMITInfo,
					MessageExpiration:  validExpiration,
					MessageID:          validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "empty_pub_key",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:       psp.ID,
					GatewayMerchantID: validMerchantID,
					TransactionInfo:   validTransactionInfo,
					MITInfo:           validMITInfo,
					MessageExpiration: validExpiration,
					MessageID:         validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "empty_recipient_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "empty_gw_merchant_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
		{
			name: "no_relationship_id",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo:             validTransactionInfo,
					MITInfo:                     validMITInfo,
					MessageExpiration:           validExpiration,
					MessageID:                   validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
			},
			expectError: true,
		},
		{
			name: "zero_amount_but_mit_not_allowed",
			req: &VisaCheckoutRequest{
				CheckoutRequestBase: CheckoutRequestBase{
					RecipientID:                 psp.ID,
					GatewayMerchantID:           validMerchantID,
					RecipientPublicKey:          psp.GetSerializedPublicKey(),
					RecipientPublicKeySignature: psp.GetKeySignature(),
					TransactionInfo: TransactionInfo{
						Amount:   0,
						Currency: "RUB",
					},
					MessageExpiration: validExpiration,
					MessageID:         validMessageID,
				},
				ProvisionedTokenID:  validCardID,
				ClientPaymentDataID: clientPaymentDataID,
				RelationshipID:      te.GetVisaAllowedRelationshipID(),
			},
			expectError: true,
		},
	}

	testVisaCheckout := func(t *testing.T, tc testCase) {
		resp, err := te.Client.VisaCheckout(te.BaseCtx, tc.req)
		if tc.expectError {
			require.Error(t, err)
			return
		}
		require.NoError(t, err)

		recipient := paymenttoken.NewRecipient(psp.ID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(psp.Key))
		token := &paymenttoken.EncryptedMessage{}
		err = recipient.UnsealJSONMessage(resp.PaymentToken, token)
		require.NoError(t, err)

		assert.Equal(t, tc.req.MessageID, token.MessageID)
		assert.Equal(t, tc.req.GatewayMerchantID, token.GatewayMerchantID)
		assert.Equal(t, strconv.FormatInt(tc.req.MessageExpiration, 10), token.MessageExpiration)
		assert.Equal(t, 499, token.TransactionDetails.Amount)
		assert.Equal(t, "USD", token.TransactionDetails.Currency)
		assert.Equal(t, paymenttoken.PaymentMethodCard, token.PaymentMethod)
		assert.NotEmpty(t, token.PaymentAccountReference)
		assert.Equal(t, validCardExpirationYear, token.PaymentMethodDetails.ExpirationYear)
		assert.Equal(t, validCardExpirationMonth, token.PaymentMethodDetails.ExpirationMonth)
		assert.NotEmpty(t, token.PaymentMethodDetails.PAN)
		assert.Equal(t, paymenttoken.AuthMethodCloudToken, token.PaymentMethodDetails.AuthMethod)
		assert.NotEmpty(t, token.PaymentMethodDetails.Cryptogram)
		assert.NotEmpty(t, token.PaymentMethodDetails.ECI)
		assert.Equal(t, token.MITDetails.Recurring, true)
		assert.Equal(t, token.MITDetails.Deferred, true)
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			testVisaCheckout(t, tc)
		})
	}

	t.Run("mitInfoOmitted", func(t *testing.T) {
		req := &VisaCheckoutRequest{
			CheckoutRequestBase: CheckoutRequestBase{
				RecipientID:                 psp.ID,
				GatewayMerchantID:           validMerchantID,
				RecipientPublicKey:          psp.GetSerializedPublicKey(),
				RecipientPublicKeySignature: psp.GetKeySignature(),
				TransactionInfo:             validTransactionInfo,
				MessageExpiration:           validExpiration,
				MessageID:                   validMessageID,
			},
			ProvisionedTokenID:  validCardID,
			ClientPaymentDataID: clientPaymentDataID,
			RelationshipID:      te.GetVisaAllowedRelationshipID(),
		}
		resp, err := te.Client.VisaCheckout(te.BaseCtx, req)
		require.NoError(t, err)

		recipient := paymenttoken.NewRecipient(psp.ID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(psp.Key))
		token := &paymenttoken.EncryptedMessage{}
		err = recipient.UnsealJSONMessage(resp.PaymentToken, token)
		require.NoError(t, err)

		assert.Empty(t, token.MITDetails)
	})
}

func TestNoVisaHandlersAvailable(t *testing.T) {
	te, err := NewCustomTestEnv(t, false)
	require.NoError(t, err)
	defer te.Close()

	validCardExpirationYear := 2020
	validCardExpirationMonth := 1
	validCard := visa.Card{
		PrimaryAccountNumber: "2222222222222222",
		PanExpirationMonth:   strconv.Itoa(validCardExpirationMonth),
		PanExpirationYear:    strconv.Itoa(validCardExpirationYear),
	}

	enrollRequest := &VisaEnrollRequest{
		Card:      validCard,
		EmailHash: "61b177aa2ec5022af4bbe431581e7f79a1d361c1432ed646cb9236e31651a162",
		Locale:    "ru_RU",
		PANSource: visa.PANSourceOnfile,
		AccountID: genUUID(),
	}

	_, err = te.Client.VisaEnroll(te.BaseCtx, enrollRequest)
	require.Error(t, err, "all visa request must fail")
	require.Equal(t, err.Error(), "visa/enroll: Unexpected status: 404 Not Found")

	validExpiration := time.Now().Add(time.Hour).Unix() * 1000
	validMerchantID := genUUID()
	validMessageID := genUUID()
	psp := newTestPaymentGateway(te)
	validTransactionInfo := TransactionInfo{
		Currency: "USD",
		Amount:   499,
	}

	checkoutRequest := &VisaCheckoutRequest{
		CheckoutRequestBase: CheckoutRequestBase{
			RecipientID:        psp.ID,
			GatewayMerchantID:  validMerchantID,
			RecipientPublicKey: psp.GetSerializedPublicKey(),
			TransactionInfo:    validTransactionInfo,
			MessageExpiration:  validExpiration,
			MessageID:          validMessageID,
		},
		ProvisionedTokenID: "provisioned-token-id",
	}

	_, err = te.Client.VisaCheckout(te.BaseCtx, checkoutRequest)
	require.Error(t, err, "all visa request must fail")
	require.Equal(t, err.Error(), "visa/checkout: Unexpected status: 404 Not Found")

	signRequest := &SignRequest{
		URL:    "http://ya.ru",
		Method: "GET",
		Body:   "",
	}
	_, err = te.Client.VisaSignRequest(te.BaseCtx, signRequest)
	require.Error(t, err, "all visa request must fail")
	require.Equal(t, err.Error(), "visa/sign_request: Unexpected status: 404 Not Found")
}

func TestMastercardShouldWorkWithoutVisa(t *testing.T) {
	te, err := NewCustomTestEnv(t, false)
	require.NoError(t, err)
	defer te.Close()

	enrollRequest := &MastercardEnrollRequest{
		Card: mastercard.Card{
			PrimaryAccountNumber: genRandomPAN(),
			PanExpirationMonth:   "01",
			PanExpirationYear:    "2022",
			CardholderFullName:   "John Donne",
			CardSecurityCode:     "123",
		},
		CardSource: mastercard.CardSourceMerchant,
		AccountID:  "uid-hash",
	}

	_, err = te.Client.MastercardEnroll(te.BaseCtx, enrollRequest)
	require.NoError(t, err)
}

func TestMastercardEnrollWithoutCardholder(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	enrollRequest := &MastercardEnrollRequest{
		Card: mastercard.Card{
			PrimaryAccountNumber: genRandomPAN(),
			PanExpirationMonth:   "01",
			PanExpirationYear:    "2022",
			CardSecurityCode:     "123",
		},
		CardSource: mastercard.CardSourceMerchant,
		AccountID:  "uid-hash",
	}

	_, err := te.Client.MastercardEnroll(te.BaseCtx, enrollRequest)
	require.NoError(t, err)
}

func TestExternalHandler_PaymentTokenVerifyRecipientKey(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	psp := newTestPaymentGateway(te)

	verifyReq := &PaymentTokenVerifyRecipientKeyRequest{
		RecipientPublicKey:          psp.GetSerializedPublicKey(),
		RecipientPublicKeySignature: psp.GetKeySignature(),
	}
	err := te.Client.PaymentTokenVerifyRecipientKey(te.BaseCtx, verifyReq)
	assert.NoError(t, err)

	verifyReq = &PaymentTokenVerifyRecipientKeyRequest{
		RecipientPublicKey: psp.GetSerializedPublicKey(),
	}
	err = te.Client.PaymentTokenVerifyRecipientKey(te.BaseCtx, verifyReq)
	assert.Error(t, err)

	verifyReq = &PaymentTokenVerifyRecipientKeyRequest{
		RecipientPublicKey:          psp.GetSerializedPublicKey(),
		RecipientPublicKeySignature: "..",
	}
	err = te.Client.PaymentTokenVerifyRecipientKey(te.BaseCtx, verifyReq)
	assert.Error(t, err)
}
