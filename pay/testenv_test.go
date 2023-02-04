package mastercard_test

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	zaplog "go.uber.org/zap"
	"gopkg.in/square/go-jose.v2"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"a.yandex-team.ru/pay/duckgo/mastercard"
	"a.yandex-team.ru/pay/duckgo/mastercard/mcmock"
)

type TestEnv struct {
	t         *testing.T
	Client    *mastercard.Client
	SRCServer *mcmock.SRCServer

	clientSigningKey    *rsa.PrivateKey
	clientEncryptionKey *rsa.PrivateKey

	ctx       context.Context
	cancelCtx context.CancelFunc
}

func NewTestEnv(t *testing.T) *TestEnv {
	ctx, cancel := context.WithCancel(context.Background())
	te := &TestEnv{
		t: t,

		ctx:       ctx,
		cancelCtx: cancel,
	}

	logger := mustCreateTestLogger()
	te.SRCServer = mcmock.NewSRCServer(logger)

	var err error
	te.clientSigningKey, err = rsa.GenerateKey(rand.Reader, 2048)
	require.NoError(t, err)
	te.clientEncryptionKey, err = rsa.GenerateKey(rand.Reader, 2048)
	require.NoError(t, err)

	const serviceID = "ServiceID"

	te.Client = mastercard.NewClient(&mastercard.Config{
		Logger:        mustCreateTestLogger(),
		APIHostURL:    te.SRCServer.GetBaseURL(),
		PublicKeysURL: te.SRCServer.GetKeysURL(),

		ConsumerKey:   "ConsumerKey",
		ClientID:      "ClientID",
		ServiceID:     serviceID,
		SigningKey:    te.clientSigningKey,
		EncryptionKey: te.clientEncryptionKey,

		KeysInitDownloadTimeout: 30 * time.Second,
	})
	te.SRCServer.RegisterService(serviceID, &mcmock.Service{
		EncryptionKey: jose.JSONWebKey{
			Key:       te.clientEncryptionKey.Public(),
			KeyID:     "service-kid",
			Algorithm: "RSA-OAEP-256",
			Use:       "enc",
		},
	})
	_ = te.Client.HealthCheck(ctx)

	return te
}

func (te *TestEnv) Close() {
	te.cancelCtx()
	te.SRCServer.Close()
	te.Client.Close()
}

func mustCreateTestLogger() log.Logger {
	logger, err := zap.New(zaplog.NewDevelopmentConfig())
	if err != nil {
		panic(err)
	}
	return logger
}

func TestMastercardValidateCard(t *testing.T) {
	req := &mastercard.EnrollCardRequest{
		Consumer: mastercard.Consumer{
			ConsumerIdentity: mastercard.ConsumerIdentity{
				IdentityType:  mastercard.IdentityTypeExternalAccountID,
				IdentityValue: "yandex-uid",
			},
		},
		CardSource: mastercard.CardSourceCardholder,
	}
	card := &mastercard.Card{
		PrimaryAccountNumber: "5204731600014792",
		PanExpirationMonth:   "12",
		PanExpirationYear:    "2030",
		CardholderFullName:   "test",
	}

	te := NewTestEnv(t)
	defer te.Close()

	encryptedCard, err := te.Client.EncryptCard(card)
	require.NoError(t, err)
	req.EncryptedCard = encryptedCard

	resp, err := te.Client.EnrollCard(te.ctx, req)
	if err != nil {
		t.Errorf("EncryptCard() error = %v", err)
		return
	}
	assert.NotEmpty(t, resp.SRCCorrelationID)
	assert.NotEmpty(t, resp.MaskedCard.SRCDigitalCardID)
	assert.NotEmpty(t, resp.MaskedCard.PANBin)
	assert.NotEmpty(t, resp.MaskedCard.PANLastFour)
	assert.NotEmpty(t, resp.MaskedCard.DateOfCardCreated)
	assert.NotEmpty(t, resp.MaskedCard.DigitalCardData.Status)
	assert.NotEmpty(t, resp.MaskedCard.DigitalCardData.DescriptorName)
	assert.NotEmpty(t, resp.MaskedCard.DigitalCardData.ArtURI)

	req2 := &mastercard.CheckoutRequest{
		SRCDigitalCardID: resp.MaskedCard.SRCDigitalCardID,
		DPATransactionOptions: mastercard.DPATransactionOptions{
			ThreeDSPreference: mastercard.ThreeDSPreferenceNone,
		},
	}

	resp2, err := te.Client.Checkout(te.ctx, req2)
	require.NoError(t, err)

	assert.NotEmpty(t, resp2.SRCCorrelationID)
	assert.NotEmpty(t, resp2.MaskedCard.SRCDigitalCardID)
	assert.NotEmpty(t, resp2.MaskedCard.PANBin)
	assert.NotEmpty(t, resp2.MaskedCard.PANLastFour)
	assert.NotEmpty(t, resp2.MaskedCard.DateOfCardCreated)
	assert.NotEmpty(t, resp2.MaskedCard.DigitalCardData.Status)
	assert.NotEmpty(t, resp2.MaskedCard.DigitalCardData.DescriptorName)
	assert.NotEmpty(t, resp2.MaskedCard.DigitalCardData.ArtURI)
	assert.NotEmpty(t, resp2.Cryptogram)
	assert.NotEmpty(t, resp2.Token)
}
