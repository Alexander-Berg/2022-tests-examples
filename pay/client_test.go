package mastercard_test

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
	"time"

	oauth "github.com/mastercard/oauth1-signer-go"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/mastercard"
)

func TestClient_EncryptCard(t *testing.T) {
	tests := []struct {
		name    string
		card    *mastercard.Card
		wantErr bool
	}{
		{
			name: "simple",
			card: &mastercard.Card{
				PrimaryAccountNumber: "123",
			},
		},
		{
			name: "full",
			card: &mastercard.Card{
				PrimaryAccountNumber: "123",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "30",
				CardholderFullName:   "Ivan Petrov",
				CardSecurityCode:     "123",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			ct, err := te.Client.EncryptCard(tt.card)
			if (err != nil) != tt.wantErr {
				t.Errorf("EncryptCard() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			card, err := te.SRCServer.DecryptCard(ct)
			require.NoError(t, err)
			assert.Equal(t, tt.card, card)
		})
	}
}

func TestClient_EnrollCard(t *testing.T) {
	tests := []struct {
		name string
		req  *mastercard.EnrollCardRequest
		card *mastercard.Card
	}{
		{
			name: "simple",
			req: &mastercard.EnrollCardRequest{
				Consumer: mastercard.Consumer{
					ConsumerIdentity: mastercard.ConsumerIdentity{
						IdentityType:  mastercard.IdentityTypeExternalAccountID,
						IdentityValue: "yandex-uid",
					},
				},
				CardSource: mastercard.CardSourceCardholder,
			},
			card: &mastercard.Card{
				PrimaryAccountNumber: "5204731600014792",
				PanExpirationMonth:   "12",
				PanExpirationYear:    "2030",
				CardholderFullName:   "test",
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			encryptedCard, err := te.Client.EncryptCard(tt.card)
			require.NoError(t, err)

			req := *tt.req
			req.EncryptedCard = encryptedCard

			resp, err := te.Client.EnrollCard(te.ctx, &req)
			if err != nil {
				t.Errorf("EncryptCard() error = %v", err)
				return
			}
			assert.NotEmpty(t, resp.MaskedCard.SRCDigitalCardID)

			_, err = te.Client.Checkout(te.ctx, &mastercard.CheckoutRequest{SRCDigitalCardID: resp.MaskedCard.SRCDigitalCardID})
			require.NoError(t, err)
		})
	}
}

func TestClient_MakeAuthorizationHeader(t *testing.T) {
	tests := []struct {
		name    string
		payload []byte
	}{
		{
			name:    "no_body",
			payload: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			te := NewTestEnv(t)
			defer te.Close()

			u, err := url.Parse(te.SRCServer.GetBaseURL())
			require.NoError(t, err)
			u.Path = "/check_oauth"

			authHeader, err := te.Client.MakeAuthorizationHeader(u, http.MethodPost, tt.payload)
			if err != nil {
				t.Errorf("MakeAuthorizationHeader() error = %v", err)
				return
			}

			req, err := http.NewRequest(http.MethodPost, u.String(), bytes.NewBuffer(tt.payload))
			require.NoError(t, err)
			req.Header.Set(oauth.AuthorizationHeaderName, authHeader)
			resp, err := http.DefaultClient.Do(req)
			require.NoError(t, err)
			_ = resp.Body.Close()
			assert.Equal(t, http.StatusOK, resp.StatusCode)
		})
	}
}

func TestClient_CheckoutRedirect(t *testing.T) {
	te := NewTestEnv(t)
	defer te.Close()

	handler := func(rw http.ResponseWriter, req *http.Request) {
		http.Redirect(rw, req, te.SRCServer.GetBaseURL()+"/404", http.StatusFound)
	}
	server := httptest.NewServer(http.HandlerFunc(handler))
	defer server.Close()

	client := mastercard.NewClient(&mastercard.Config{
		Logger:        mustCreateTestLogger(),
		APIHostURL:    server.URL,
		PublicKeysURL: te.SRCServer.GetKeysURL(),

		ConsumerKey:   "ConsumerKey",
		ClientID:      "ClientID",
		ServiceID:     "ServiceID",
		SigningKey:    te.clientSigningKey,
		EncryptionKey: te.clientEncryptionKey,

		KeysInitDownloadTimeout: 30 * time.Second,
	})

	_, err := client.Checkout(context.Background(), &mastercard.CheckoutRequest{SRCDigitalCardID: "123"})
	require.Error(t, err)
	mcErr, ok := err.(*mastercard.Error)
	require.True(t, ok)
	assert.Equal(t, 404, mcErr.GetStatusCode())
}
