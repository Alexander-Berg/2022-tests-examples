package visa_test

import (
	"context"
	"fmt"
	"math/rand"
	"testing"
	"time"

	zaplog "go.uber.org/zap"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"a.yandex-team.ru/pay/duckgo/visa"
	"a.yandex-team.ru/pay/duckgo/visa/visamock"
)

type TestEnv struct {
	t         *testing.T
	Client    *visa.Client
	VTSServer *visamock.VTSServer

	SigningKey    *visa.SharedKey
	EncryptionKey *visa.SharedKey
	ctx           context.Context
	cancelCtx     context.CancelFunc

	relationshipID string
}

func NewTestEnv(t *testing.T) *TestEnv {
	return NewTestEnvWithKeys(t,
		&visa.SharedKey{
			KeyID:  "key1",
			Secret: "secret1",
		}, &visa.SharedKey{
			KeyID:  "ver-key",
			Secret: "ver-secret",
		}, &visa.SharedKey{
			KeyID:  "enc-key",
			Secret: "enc-secret",
		})
}

func NewTestEnvWithKeys(
	t *testing.T,
	signingKey *visa.SharedKey,
	verifyingKey *visa.SharedKey,
	encryptionKey *visa.SharedKey,
) *TestEnv {
	ctx, cancel := context.WithCancel(context.Background())
	relationshipID := fmt.Sprintf("%d-%d", rand.Int(), rand.Int())
	te := &TestEnv{
		t: t,

		ctx:            ctx,
		cancelCtx:      cancel,
		relationshipID: relationshipID,
	}

	te.SigningKey = signingKey
	te.EncryptionKey = encryptionKey

	logger := mustCreateTestLogger()
	te.VTSServer = visamock.NewVTSServer(logger,
		visamock.WithSigningKey(signingKey),
		visamock.WithEncryptionKey(encryptionKey),
		visamock.WithAllowedRelationshipID(relationshipID),
	)

	te.Client = visa.NewClient(&visa.Config{
		Logger:        mustCreateTestLogger(),
		APIHostURL:    te.VTSServer.GetBaseURL(),
		ClientAppID:   "visa-test",
		SigningKey:    signingKey,
		VerifyingKeys: []*visa.SharedKey{verifyingKey},
		EncryptionKey: encryptionKey,
		EnrollTimeout: 100 * time.Second,
	})

	return te
}

func (te *TestEnv) Close() {
	te.cancelCtx()
	te.VTSServer.Close()
	te.Client.Close()
}

func (te *TestEnv) GetAllowedRelationshipID() string {
	return te.relationshipID
}

func mustCreateTestLogger() log.Logger {
	logger, err := zap.New(zaplog.NewDevelopmentConfig())
	if err != nil {
		panic(err)
	}
	return logger
}
