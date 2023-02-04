package visa_test

import (
	"bytes"
	"context"
	"net/http"
	"net/url"
	"testing"

	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/visa"
)

const (
	emailHash = "61b177aa2ec5022af4bbe431581e7f79a1d361c1432ed646cb9236e31651a162"
	accountID = "test-account"
	locale    = "ru_RU"
)

var justCard = visa.Card{
	PrimaryAccountNumber: "5204731600014792",
	PanExpirationMonth:   "12",
	PanExpirationYear:    "2030",
}

var cvvCard = visa.Card{
	PrimaryAccountNumber: "5204731600014792",
	PanExpirationMonth:   "12",
	PanExpirationYear:    "2030",
	CVV2:                 "777",
}

func TestClient_EnrollCard(t *testing.T) {
	tests := []struct {
		name      string
		card      *visa.Card
		panSource visa.PANSource
		isAPIErr  bool
	}{
		{
			name:      "simple",
			card:      &justCard,
			panSource: visa.PANSourceOnfile,
			isAPIErr:  false,
		},
		{
			name:      "simpleWithCVV",
			card:      &cvvCard,
			panSource: visa.PANSourceManuallyEntered,
			isAPIErr:  false,
		},
		{
			name: "wrongPAN",
			card: &visa.Card{
				PrimaryAccountNumber: "1",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2033",
			},
			panSource: visa.PANSourceOnfile,
			isAPIErr:  true,
		},
		{
			name: "missingPAN",
			card: &visa.Card{
				PrimaryAccountNumber: "5204731600019999",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2033",
			},
			panSource: visa.PANSourceOnfile,
			isAPIErr:  true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			req, err := te.Client.CreateEnrollRequest(accountID, emailHash, locale, tt.panSource, tt.card)
			require.NoError(t, err)

			resp, err := te.Client.EnrollCard(te.ctx, te.GetAllowedRelationshipID(), req)

			if tt.isAPIErr {
				assert.Error(t, err, "enroll should fail")
				_, ok := err.(*visa.Error)
				assert.True(t, ok, "error must be instance of shared.APIError")
				return
			}

			require.NoError(t, err)

			assert.NotEmpty(t, resp.VTSRequestID)
			assert.NotEmpty(t, resp.VTSResponseID)
			assert.NotEmpty(t, resp.VTSCorrelationID)
			assert.NotEmpty(t, resp.PaymentInstrument.Last4)

			assert.NotEmpty(t, resp.TokenInfo.EncTokenInfo)
			token, err := te.Client.DecryptToken(resp.TokenInfo.EncTokenInfo)
			assert.NoError(t, err)
			assert.NotEmpty(t, token.Value)

			expectedToken := te.VTSServer.GetOrCreateToken(tt.card.PrimaryAccountNumber)
			require.Equal(t, expectedToken, token.Value)
		})
	}
}

func TestClient_EnrollCardCreateRequest(t *testing.T) {
	tests := []struct {
		name      string
		card      *visa.Card
		panSource visa.PANSource
		isError   bool
	}{
		{
			name: "simple",
			card: &visa.Card{
				PrimaryAccountNumber: "5204731600014792",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2030",
			},
			panSource: visa.PANSourceOnfile,
			isError:   false,
		},
		{
			name: "cvv",
			card: &visa.Card{
				PrimaryAccountNumber: "5204731600014792",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2030",
				CVV2:                 "777",
			},
			panSource: visa.PANSourceManuallyEntered,
			isError:   false,
		},
		{
			name: "wrong_cvv_onfile",
			card: &visa.Card{
				PrimaryAccountNumber: "5204731600014792",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2030",
				CVV2:                 "777",
			},
			panSource: visa.PANSourceOnfile,
			isError:   true,
		},
		{
			name: "wrong_no_cvv_manual",
			card: &visa.Card{
				PrimaryAccountNumber: "5204731600014792",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2030",
			},
			panSource: visa.PANSourceManuallyEntered,
			isError:   true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			req, err := te.Client.CreateEnrollRequest(accountID, emailHash, locale, tt.panSource, tt.card)
			if tt.isError {
				require.Error(t, err)
				return
			}

			require.NoError(t, err)
			if tt.card.CVV2 != "" {
				require.NotEmpty(t, req.EncRiskDataInfo, "encrypted risk data expected")
				var riskData []riskData
				err = te.Client.DecryptAndUnmarshall(req.EncRiskDataInfo, &riskData)
				require.NoError(t, err)
				require.NotEmpty(t, riskData)
				require.Equal(t, "cvv2", riskData[0].Name)
				require.Equal(t, tt.card.CVV2, riskData[0].Value)
			}

		})
	}
}

func TestClient_MakeAuthorizationHeader(t *testing.T) {
	encKey := &visa.SharedKey{
		KeyID:  "kid_end",
		Secret: "sec_enc",
	}

	verKey := &visa.SharedKey{
		KeyID:  "kid_ver",
		Secret: "sec_ver",
	}

	tests := []struct {
		name             string
		payload          []byte
		serverSigningKey *visa.SharedKey
		clientSigningKey *visa.SharedKey
		expectError      bool
	}{
		{
			name:    "no_body",
			payload: nil,
			serverSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			clientSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			expectError: false,
		},
		{
			name:    "some_body",
			payload: []byte("some_data"),
			serverSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			clientSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			expectError: false,
		},
		{
			name:    "wrong_secret",
			payload: []byte("some_data"),
			serverSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			clientSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "another_sec1",
			},
			expectError: true,
		},
		{
			name:    "wrong_key",
			payload: []byte("some_data"),
			serverSigningKey: &visa.SharedKey{
				KeyID:  "kid1",
				Secret: "sec1",
			},
			clientSigningKey: &visa.SharedKey{
				KeyID:  "another_kid1",
				Secret: "sec1",
			},
			expectError: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnvWithKeys(t, tt.serverSigningKey, verKey, encKey)
			defer te.Close()

			u, err := url.Parse(te.VTSServer.GetBaseURL())
			require.NoError(t, err)
			u.Path = "/check_oauth"

			req, err := http.NewRequest(http.MethodPost, u.String(), bytes.NewBuffer(tt.payload))
			require.NoError(t, err)
			req.Header.Set("X-Request-Id", genUUID())

			signer := visa.NewSigner(
				visa.WithSignerAPIKey(tt.clientSigningKey.KeyID),
				visa.WithSignerSecret(tt.clientSigningKey.Secret))
			err = signer.Sign(req)
			require.NoError(t, err)
			req.Header.Set("X-Request-Id", genUUID())

			resp, err := http.DefaultClient.Do(req)
			defer func() {
				_ = resp.Body.Close()
			}()

			require.NoError(t, err)
			if !tt.expectError {
				require.Equal(t, http.StatusOK, resp.StatusCode)
			} else {
				require.Equal(t, http.StatusUnauthorized, resp.StatusCode)
			}
		})
	}
}

func TestClient_GetPaymentData(t *testing.T) {
	type provisionIDFunc = func(context.Context, *TestEnv) string

	getProvisionIDViaEnroll := func(ctx context.Context, te *TestEnv) string {
		enrollReq, err := te.Client.CreateEnrollRequest(accountID, emailHash, "ru_RU", visa.PANSourceOnfile, &justCard)
		require.NoError(t, err)

		enrollResp, err := te.Client.EnrollCard(ctx, te.GetAllowedRelationshipID(), enrollReq)
		require.NoError(t, err)

		return enrollResp.ProvisionedTokenID
	}

	getWrongProvisionID := func(ctx context.Context, te *TestEnv) string {
		return "does_not_exists"
	}

	tests := []struct {
		name         string
		getProvision provisionIDFunc
		isError      bool
	}{
		{
			name:         "simple",
			getProvision: getProvisionIDViaEnroll,
			isError:      false,
		},
		{
			name:         "missingID",
			getProvision: getWrongProvisionID,
			isError:      true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			provisionID := tt.getProvision(te.ctx, te)
			pdReq := &visa.PaymentDataRequest{
				ClientPaymentDataID: genUUID(),
				PaymentRequest: visa.PaymentRequest{
					TransactionType: visa.TransactionTypeECOM,
				},
			}

			pdResp, err := te.Client.GetPaymentData(te.ctx, te.GetAllowedRelationshipID(), provisionID, pdReq)
			if tt.isError {
				require.Error(t, err)
				return
			}

			require.NoError(t, err)
			require.NotEmpty(t, pdResp.CryptogramInfo.Cryptogram, "cryptogram is required")
			require.NotEmpty(t, pdResp.TokenInfo.EncTokenInfo, "encrypted token info is required")
			require.NotEmpty(t, pdResp.TokenInfo.Last4, "token last4 required")
			require.NotEmpty(t, pdResp.TokenInfo.ExpirationDate.Year, "token expiration date required")
			require.NotEmpty(t, pdResp.TokenInfo.ExpirationDate.Month, "token expiration date required")
			require.NotEmpty(t, pdResp.PaymentInstrument.PaymentAccountReference, "par required")
			require.NotEmpty(t, pdResp.PaymentDataID, "payment data id is required")
			require.NotEmpty(t, pdResp.VTSCorrelationID)
		})
	}
}

type riskData struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

func genUUID() string {
	id, err := uuid.NewV4()
	if err != nil {
		panic(err)
	}

	return id.String()
}
