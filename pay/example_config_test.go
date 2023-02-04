package exampleconfigs

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/yatest"
	"a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/pay/duckgo/server"
)

func TestExampleConfig(t *testing.T) {
	config := server.NewConfig()
	err := config.ParseFromFile(yatest.SourcePath("pay/duckgo/example-configs/config.yml"))
	require.NoError(t, err)

	expected := &server.Config{
		InternalAPI: server.ConfigInternalAPI{
			ListenAddr: "[::1]:2020",
			Auth:       "shared_key",

			SharedKey: server.ConfigSharedKey{
				SharedKeyPath: "",
			},
		},
		ExternalAPI: server.ConfigExternalAPI{
			CertFile:   "example-configs/cert.cer",
			KeyFile:    "example-configs/cert.key",
			ListenAddr: ":1867",
			DisableTLS: false,
			Auth:       "shared_key",
			SharedKey: server.ConfigSharedKey{
				SharedKeyPath: "",
			},
			TVM: server.ConfigTVM{
				Self:       "duckgo",
				TVMToolURL: "http://localhost:1998",
				AuthToken:  "123",
				Allowed:    []tvm.ClientID{123},
			},
		},
		Mastercard: server.ConfigMastercard{
			APIHostURL:              "https://sandbox.api.mastercard.com",
			PublicKeysURL:           "https://sandbox.src.mastercard.com/keys",
			KeysUpdatePeriod:        "168h",
			KeysInitDownloadTimeout: "15s",
			ConsumerKey:             "",
			ConsumerKeyPath:         "",
			ClientID:                "",
			ClientIDPath:            "",
			ServiceID:               "",
			ServiceIDPath:           "",
			SigningKeyPath:          "example-configs/mastercard-sign-key.pem",
			EncryptionKeyPath:       "example-configs/mastercard-encryption-key.pem",
		},
		Visa: server.ConfigVisa{
			IsEnabled:     true,
			APIHostURL:    "https://cert.api.visa.com",
			ClientAppID:   "YandexPay",
			EnrollTimeout: "16s",
			SigningKey:    server.ConfigNamedSharedKey{},
			VerifyingKeys: []server.ConfigNamedSharedKey{{}},
			EncryptionKey: server.ConfigNamedSharedKey{},
		},
		PaymentToken: server.ConfigPaymentToken{
			SigningKeyPath:       "example-configs/paymenttoken-intermediate-key.pem",
			IntermediateCertPath: "example-configs/paymenttoken-intermediate.json",

			RecipientVerificationPublicKeys: []string{
				"example-configs/recipient-verification-key-pub-1.pem",
			},
		},
		Wallet: server.ConfigWallet{
			Thales: server.ConfigThales{
				CardEncryptionCertPath: "example-configs/thales-card-encryption-cert.pem",
			},
		},
		Logger: server.ConfigLogger{
			Level: "debug",
			Sink:  "stdout",
			Syslog: server.ConfigSyslog{
				Network:    "",
				RemoteAddr: "",
			},
		},
	}

	assert.Equal(t, expected, config)
}
