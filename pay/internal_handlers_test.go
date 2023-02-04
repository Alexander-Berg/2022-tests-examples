package server

import (
	"encoding/json"
	"path"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/paymenttoken"
)

func Test_getCardLastFour(t *testing.T) {
	tests := []struct {
		pan  string
		want string
	}{
		{"1234", "1234"},
		{"123", "123"},
		{"", ""},
		{"12345", "2345"},
		{"123456789", "6789"},
	}
	for _, tt := range tests {
		t.Run(tt.pan, func(t *testing.T) {
			if got := getCardLastFour(tt.pan); got != tt.want {
				t.Errorf("getCardLastFour() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPANCheckout(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	type testCase struct {
		name        string
		req         *PANCheckoutRequest
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
	validCard := PANCheckoutCard{
		PrimaryAccountNumber: "1111111111111111",
		PanExpirationMonth:   12,
		PanExpirationYear:    2030,
	}
	validMITInfo := &MITInfo{
		Recurring: true,
		Deferred:  true,
	}

	testCases := []testCase{
		{
			name: "empty_card",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card:               PANCheckoutCard{},
			},
			expectError: true,
		},
		{
			name: "empty_pan",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card: PANCheckoutCard{
					PanExpirationMonth: 9,
					PanExpirationYear:  2025,
				},
			},
			expectError: true,
		},
		{
			name: "empty_card_month",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card: PANCheckoutCard{
					PrimaryAccountNumber: "1111111111111111",
					PanExpirationMonth:   0,
					PanExpirationYear:    2031,
				},
			},
			expectError: true,
		},
		{
			name: "empty_card_year",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card: PANCheckoutCard{
					PrimaryAccountNumber: "1111111111111111",
					PanExpirationMonth:   0,
					PanExpirationYear:    2031,
				},
			},
			expectError: true,
		},
		{
			name: "legit",
			req: &PANCheckoutRequest{
				RecipientID:                 psp.ID,
				GatewayMerchantID:           validMerchantID,
				RecipientPublicKey:          psp.GetSerializedPublicKey(),
				RecipientPublicKeySignature: psp.GetKeySignature(),
				TransactionInfo:             validTransactionInfo,
				MITInfo:                     validMITInfo,
				MessageExpiration:           validExpiration,
				MessageID:                   validMessageID,
				Card:                        validCard,
			},
		},
		{
			name: "empty_message_id",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MessageExpiration:  validExpiration,
				Card:               validCard,
			},
			expectError: true,
		},
		{
			name: "empty_expiration",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageID:          validMessageID,
				Card:               validCard,
			},
			expectError: true,
		},
		{
			name: "invalid_pub_key",
			req: &PANCheckoutRequest{
				RecipientID:        psp.ID,
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: "18387573732",
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card:               validCard,
			},
			expectError: true,
		},
		{
			name: "empty_pub_key",
			req: &PANCheckoutRequest{
				RecipientID:       psp.ID,
				GatewayMerchantID: validMerchantID,
				TransactionInfo:   validTransactionInfo,
				MITInfo:           validMITInfo,
				MessageExpiration: validExpiration,
				MessageID:         validMessageID,
				Card:              validCard,
			},
			expectError: true,
		},
		{
			name: "empty_recipient_id",
			req: &PANCheckoutRequest{
				GatewayMerchantID:  validMerchantID,
				RecipientPublicKey: psp.GetSerializedPublicKey(),
				TransactionInfo:    validTransactionInfo,
				MITInfo:            validMITInfo,
				MessageExpiration:  validExpiration,
				MessageID:          validMessageID,
				Card:               validCard,
			},
			expectError: true,
		},
		{
			name: "empty_gw_merchant_id",
			req: &PANCheckoutRequest{
				RecipientID:                 psp.ID,
				RecipientPublicKey:          psp.GetSerializedPublicKey(),
				RecipientPublicKeySignature: psp.GetKeySignature(),
				TransactionInfo:             validTransactionInfo,
				MITInfo:                     validMITInfo,
				MessageExpiration:           validExpiration,
				MessageID:                   validMessageID,
				Card:                        validCard,
			},
			expectError: true,
		},
		{
			name: "zero_amount_but_mit_not_allowed",
			req: &PANCheckoutRequest{
				GatewayMerchantID:           validMerchantID,
				RecipientID:                 psp.ID,
				RecipientPublicKey:          psp.GetSerializedPublicKey(),
				RecipientPublicKeySignature: psp.GetKeySignature(),
				TransactionInfo: TransactionInfo{
					Amount:   0,
					Currency: "RUB",
				},
				MessageExpiration: validExpiration,
				MessageID:         validMessageID,
				Card:              validCard,
			},
			expectError: true,
		},
	}

	testPANCheckout := func(t *testing.T, tc testCase) {
		resp, err := te.Client.PANCheckout(te.BaseCtx, tc.req)
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
		assert.Empty(t, token.PaymentAccountReference)
		assert.Equal(t, tc.req.Card.PanExpirationYear, token.PaymentMethodDetails.ExpirationYear)
		assert.Equal(t, tc.req.Card.PanExpirationMonth, token.PaymentMethodDetails.ExpirationMonth)
		assert.Equal(t, tc.req.Card.PrimaryAccountNumber, token.PaymentMethodDetails.PAN)
		assert.Equal(t, paymenttoken.AuthMethodPANOnly, token.PaymentMethodDetails.AuthMethod)
		assert.Empty(t, token.PaymentMethodDetails.Cryptogram)
		assert.Empty(t, token.PaymentMethodDetails.ECI)
		assert.Equal(t, token.MITDetails.Recurring, true)
		assert.Equal(t, token.MITDetails.Deferred, true)
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			testPANCheckout(t, tc)
		})
	}

	t.Run("mitInfoOmitted", func(t *testing.T) {
		req := &PANCheckoutRequest{
			RecipientID:                 psp.ID,
			GatewayMerchantID:           validMerchantID,
			RecipientPublicKey:          psp.GetSerializedPublicKey(),
			RecipientPublicKeySignature: psp.GetKeySignature(),
			TransactionInfo:             validTransactionInfo,
			MessageExpiration:           validExpiration,
			MessageID:                   validMessageID,
			Card:                        validCard,
		}
		resp, err := te.Client.PANCheckout(te.BaseCtx, req)
		require.NoError(t, err)

		recipient := paymenttoken.NewRecipient(psp.ID, te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(psp.Key))
		token := &paymenttoken.EncryptedMessage{}
		err = recipient.UnsealJSONMessage(resp.PaymentToken, token)
		require.NoError(t, err)

		assert.Empty(t, token.MITDetails)
	})
}

func TestThalesEncryptedCard(t *testing.T) {
	runOnUnix(t)

	keyPath := path.Join(t.TempDir(), "key.pem")
	certPath := path.Join(t.TempDir(), "cert.pem")

	generateKeys(t, keyPath, certPath)

	te := NewTestEnv(t, testConfigOptionFunc(func(c *Config) {
		c.Wallet.Thales.CardEncryptionCertPath = certPath
	}))
	defer te.Close()

	type testCase struct {
		name             string
		card             PANCheckoutCard
		expectError      bool
		expectExpiration string
		certPath         string
	}

	testCases := []testCase{
		{
			name: "complete_without_cvv",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1111111111111111",
				PanExpirationYear:    2030,
				PanExpirationMonth:   6,
			},
			expectExpiration: "0630",
			expectError:      false,
		},
		{
			name: "complete_with_cvv",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1111111111111111",
				PanExpirationYear:    2030,
				PanExpirationMonth:   6,
				CVV:                  "012",
			},
			expectExpiration: "0630",
			expectError:      false,
		},
		{
			name: "bad_pan",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1",
				PanExpirationYear:    2030,
				PanExpirationMonth:   6,
				CVV:                  "012",
			},
			expectExpiration: "0630",
			expectError:      true,
		},
		{
			name: "bad_month",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1111111111111111",
				PanExpirationYear:    2030,
				PanExpirationMonth:   0,
				CVV:                  "012",
			},
			expectExpiration: "0630",
			expectError:      true,
		},
		{
			name: "bad_year",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1111111111111111",
				PanExpirationYear:    -1,
				PanExpirationMonth:   6,
				CVV:                  "012",
			},
			expectError: true,
		},
		{
			name: "bad_certificate",
			card: PANCheckoutCard{
				PrimaryAccountNumber: "1111111111111111",
				PanExpirationYear:    2030,
				PanExpirationMonth:   6,
				CVV:                  "012",
			},
			expectExpiration: "0630",
			expectError:      true,
			certPath:         keyPath,
		},
	}

	testThalesEncryptedCard := func(t *testing.T, tc *testCase) {
		if tc.certPath != "" {
			_, err := NewCustomTestEnv(t, false, testConfigOptionFunc(func(c *Config) {
				c.Wallet.Thales.CardEncryptionCertPath = tc.certPath
			}))
			assert.Error(t, err)
			return
		}

		resp, err := te.Client.ThalesEncryptedCard(
			te.BaseCtx,
			&ThalesEncryptedCardRequest{
				Card: PANCheckoutCard{
					PrimaryAccountNumber: tc.card.PrimaryAccountNumber,
					PanExpirationYear:    tc.card.PanExpirationYear,
					PanExpirationMonth:   tc.card.PanExpirationMonth,
					CVV:                  tc.card.CVV,
				},
			},
		)

		if tc.expectError {
			require.Error(t, err)
			return
		}

		require.NoError(t, err)

		decrypted := decryptMessage(t, resp.EncryptedCard, keyPath)
		parsed := ThalesEncryptedMessage{}
		err = json.Unmarshal([]byte(decrypted), &parsed)

		require.NoError(t, err)

		assert.Equal(t, tc.card.PrimaryAccountNumber, parsed.PAN)
		assert.Equal(t, tc.expectExpiration, parsed.Expiration)
		assert.Equal(t, tc.card.CVV, parsed.CVV)
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			testThalesEncryptedCard(t, &tc)
		})
	}

}
