package server

import (
	"fmt"
	"io/ioutil"
	"os"
	"testing"

	"a.yandex-team.ru/library/go/yandex/tvm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestConfigParsing(t *testing.T) {
	externalConfig := NewConfig()
	externalConfig.ExternalAPI = ConfigExternalAPI{
		CertFile:   "example-configs/cert.cer",
		KeyFile:    "example-configs/cert.key",
		ListenAddr: ":1867",
		DisableTLS: true,
		Auth:       "shared_key",
		TVM: ConfigTVM{
			Self:       "test",
			TVMToolURL: "http://localhost:1020",
			AuthToken:  "1727",
			Allowed:    []tvm.ClientID{1, 2, 3},
		},
	}

	internalConfig := NewConfig()
	internalConfig.InternalAPI = ConfigInternalAPI{
		ListenAddr: "localhost:2020",
		Auth:       "shared_key",
	}

	paymentTokenConfig := NewConfig()
	paymentTokenConfig.PaymentToken = ConfigPaymentToken{
		SigningKeyPath:       "example-configs/paymenttoken-intermediate-key.pem",
		IntermediateCertPath: "example-configs/paymenttoken-intermediate.json",
	}

	mastercardConfig := NewConfig()
	mastercardConfig.Mastercard = ConfigMastercard{
		APIHostURL:              "https://sandbox.api.mastercard.com",
		PublicKeysURL:           "https://sandbox.src.mastercard.com/keys",
		KeysUpdatePeriod:        "150h",
		KeysInitDownloadTimeout: "1h",
		KeysCachePath:           "/var/cache/mastercard-keys.json",
		ConsumerKey:             "consumer_key",
		ClientID:                "client_id",
		ServiceID:               "service_id",
		SigningKeyPath:          "example-configs/mastercard-sign-key.pem",
		EncryptionKeyPath:       "example-configs/mastercard-encryption-key.pem",
	}

	visaConfig := NewConfig()
	visaConfig.Visa = ConfigVisa{
		IsEnabled:     true,
		APIHostURL:    "https://cert.api.visa.com",
		ClientAppID:   "YandexPay",
		EnrollTimeout: "16s",
		SigningKey: ConfigNamedSharedKey{
			KeyID:  "signing-key-id",
			Secret: "signing-key-secret",
		},
		VerifyingKeys: []ConfigNamedSharedKey{
			{
				KeyID:  "verifying-key-id",
				Secret: "verifying-key-secret",
			},
			{
				KeyID:  "verifying-key-id2",
				Secret: "verifying-key-secret2",
			},
		},
		EncryptionKey: ConfigNamedSharedKey{
			KeyID:  "enc-key-id",
			Secret: "enc-key-secret",
		},
	}

	thalesConfig := NewConfig()
	thalesConfig.Wallet.Thales = ConfigThales{
		CardEncryptionCertPath: "example-configs/thales-card-encryption-cert.pem",
	}

	testCases := []struct {
		Name      string
		RawConfig string
		Expected  *Config
	}{
		{
			Name:      "empty",
			RawConfig: "{}",
			Expected:  NewConfig(),
		},
		{
			Name: "external",
			RawConfig: `{
	"external_api": {
		"listen_addr": ":1867",
		"disable_tls": true, # TLS is enabled by-default
		"cert_file": "example-configs/cert.cer", # cert if TLS enabled
		"key_file": "example-configs/cert.key", # cert key if TLS enabled
        "tvm": {
            "self": "test",
            "tvmtool_url": "http://localhost:1020",
            "auth_token": "1727",
            "allowed": [1, 2, 3],
        },
	},
}`,
			Expected: externalConfig,
		},
		{
			Name: "internal",
			RawConfig: `{
	"internal_api": {
		"listen_addr": "localhost:2020",
	},
}`,
			Expected: internalConfig,
		},
		{
			Name: "paymenttoken",
			RawConfig: `{
  "payment_token": {
    "signing_key_path": "example-configs/paymenttoken-intermediate-key.pem",
    "intermediate_cert_path": "example-configs/paymenttoken-intermediate.json",
  },
}`,
			Expected: paymentTokenConfig,
		},
		{
			Name: "mastercard",
			RawConfig: `{
  "mastercard": {
    "api_host_url": "https://sandbox.api.mastercard.com",
    "public_keys_url": "https://sandbox.src.mastercard.com/keys",
    "keys_update_period" : "150h",
    "keys_init_download_timeout" : "1h",
    "keys_cache_path": "/var/cache/mastercard-keys.json",
    "consumer_key": "consumer_key",
    "client_id": "client_id",
    "service_id": "service_id",
    "signing_key_path": "example-configs/mastercard-sign-key.pem",
    "encryption_key_path": "example-configs/mastercard-encryption-key.pem",
  },
}`,
			Expected: mastercardConfig,
		},
		{
			Name: "visa",
			RawConfig: `{
"visa": {
    "is_enabled": true,
	"api_host_url": "https://cert.api.visa.com",
    "client_app_id": "YandexPay",
    "signing_key": {
      "key_id": "signing-key-id",
      "secret": "signing-key-secret",
    },
	"verifying_keys": [
		{
			  "key_id": "verifying-key-id",
			  "secret": "verifying-key-secret",
		},
		{
			  "key_id": "verifying-key-id2",
			  "secret": "verifying-key-secret2",
		}
	],
    "encryption_key": {
      "key_id": "enc-key-id",
      "secret": "enc-key-secret",
    },
    "enroll_timeout": "16s",
  }
}`,
			Expected: visaConfig,
		},
		{
			Name: "thales",
			RawConfig: `{
	"wallet": {
		"thales": {
			"card_encryption_cert_path": "example-configs/thales-card-encryption-cert.pem"
		}
	}
}
`, Expected: thalesConfig,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.Name, func(t *testing.T) {
			f, closer := mustWriteTempFile(tc.RawConfig)
			defer closer()

			cfg := NewConfig()
			err := cfg.ParseFromFile(f.Name())
			require.NoError(t, err)

			assert.Equal(t, cfg, tc.Expected)
		})
	}

}

func mustWriteTempFile(content string) (*os.File, func()) {
	f, err := ioutil.TempFile("", "duckgo-config-")
	if err != nil {
		panic(err)
	}

	if _, err = f.WriteString(content); err != nil {
		panic(err)
	}

	closeFunc := func() {
		_ = os.Remove(f.Name())
	}

	return f, closeFunc
}

func TestConfigMastercardSecretsFromFile(t *testing.T) {
	configTemplate := `{
  "mastercard": {
    "consumer_key_path": "%s",
    "client_id_path": "%s",
    "service_id_path": "%s",
  },
}`

	consumerKey, consumerKeyCloser := mustWriteTempFile("consumer_key")
	defer consumerKeyCloser()

	clientID, clientIDCloser := mustWriteTempFile("\nclient_id\n")
	defer clientIDCloser()

	serviceID, serviceIDCloser := mustWriteTempFile("service_id\n")
	defer serviceIDCloser()

	configRAW := fmt.Sprintf(configTemplate, consumerKey.Name(), clientID.Name(), serviceID.Name())
	f, cfgCloser := mustWriteTempFile(configRAW)
	defer cfgCloser()

	gotCfg := NewConfig()
	err := gotCfg.ParseFromFile(f.Name())
	require.NoError(t, err)

	assert.Equal(t, "client_id", gotCfg.Mastercard.ClientID)
	assert.Equal(t, "service_id", gotCfg.Mastercard.ServiceID)
	assert.Equal(t, "consumer_key", gotCfg.Mastercard.ConsumerKey)
}

func TestConfigSharedSecretsFromFile(t *testing.T) {
	configTemplate := `{
  "internal_api": {
    "shared_key": {
      "shared_key_path": "%s",
    },
  },
  "external_api": {
    "shared_key": {
      "shared_key_path": "%s",
    },
  },
}`

	external, externalCloser := mustWriteTempFile("external_key")
	defer externalCloser()

	internal, internalCloser := mustWriteTempFile("internal_key")
	defer internalCloser()

	configRAW := fmt.Sprintf(configTemplate, internal.Name(), external.Name())
	f, cfgCloser := mustWriteTempFile(configRAW)
	defer cfgCloser()

	gotCfg := NewConfig()
	err := gotCfg.ParseFromFile(f.Name())
	require.NoError(t, err)

	assert.Equal(t, "internal_key", gotCfg.InternalAPI.SharedKey.SharedKey)
	assert.Equal(t, "external_key", gotCfg.ExternalAPI.SharedKey.SharedKey)
}

func TestConfigTVMAuthTokenFromFile(t *testing.T) {
	configTemplate := `{
  "external_api": {
    "tvm": {
      "auth_token_path": "%s",
    },
  },
}`

	token, tokenCloser := mustWriteTempFile("auth_token")
	defer tokenCloser()

	configRAW := fmt.Sprintf(configTemplate, token.Name())
	f, cfgCloser := mustWriteTempFile(configRAW)
	defer cfgCloser()

	gotCfg := NewConfig()
	err := gotCfg.ParseFromFile(f.Name())
	require.NoError(t, err)

	assert.Equal(t, "auth_token", gotCfg.ExternalAPI.TVM.AuthToken)
}

func TestConfigSharedSecretsFromEnv(t *testing.T) {
	_ = os.Setenv("DUCKGO_INTERNAL_API_SHARED_KEY", "internal_key")
	_ = os.Setenv("DUCKGO_EXTERNAL_API_SHARED_KEY", "external_key")
	gotCfg := NewConfig()
	gotCfg.PopulateFromEnv()

	assert.Equal(t, "internal_key", gotCfg.InternalAPI.SharedKey.SharedKey)
	assert.Equal(t, "external_key", gotCfg.ExternalAPI.SharedKey.SharedKey)
}

func TestTVMAuthFromEnv(t *testing.T) {
	_ = os.Setenv("TVMTOOL_URL", "TVMTOOL_URL")
	_ = os.Setenv("TVMTOOL_LOCAL_AUTHTOKEN", "TVMTOOL_LOCAL_AUTHTOKEN")
	gotCfg := NewConfig()
	gotCfg.PopulateFromEnv()

	assert.Equal(t, "TVMTOOL_URL", gotCfg.ExternalAPI.TVM.TVMToolURL)
	assert.Equal(t, "TVMTOOL_LOCAL_AUTHTOKEN", gotCfg.ExternalAPI.TVM.AuthToken)
}

func TestVisaKeysFromEnv(t *testing.T) {
	_ = os.Setenv("VISA_SIGNING_KEY_ID", "VISA_SIGNING_KEY_ID")
	_ = os.Setenv("VISA_SIGNING_KEY_SECRET", "VISA_SIGNING_KEY_SECRET")
	_ = os.Setenv("VISA_VERIFYING_KEY_ID", "VISA_VERIFYING_KEY_ID")
	_ = os.Setenv("VISA_VERIFYING_KEY_SECRET", "VISA_VERIFYING_KEY_SECRET")
	_ = os.Setenv("VISA_ENCRYPTION_KEY_ID", "VISA_ENCRYPTION_KEY_ID")
	_ = os.Setenv("VISA_ENCRYPTION_KEY_SECRET", "VISA_ENCRYPTION_KEY_SECRET")

	gotCfg := NewConfig()
	gotCfg.PopulateFromEnv()

	assert.Equal(t, "VISA_SIGNING_KEY_ID", gotCfg.Visa.SigningKey.KeyID)
	assert.Equal(t, "VISA_SIGNING_KEY_SECRET", gotCfg.Visa.SigningKey.Secret)
	assert.Equal(t, "VISA_VERIFYING_KEY_ID", gotCfg.Visa.VerifyingKeys[0].KeyID)
	assert.Equal(t, "VISA_VERIFYING_KEY_SECRET", gotCfg.Visa.VerifyingKeys[0].Secret)
	assert.Equal(t, "VISA_ENCRYPTION_KEY_ID", gotCfg.Visa.EncryptionKey.KeyID)
	assert.Equal(t, "VISA_ENCRYPTION_KEY_SECRET", gotCfg.Visa.EncryptionKey.Secret)
}

func TestConfigVisaKeysFromFile(t *testing.T) {
	configTemplate := `{
"visa": {
    "is_enabled": true,
    "client_app_id": "YandexPay",
    "signing_key": {
      "key_id_path": "%s",
      "secret_path": "%s",
    },
	"verifying_keys": [
		{
		  "key_id_path": "%s",
		  "secret_path": "%s",
		},
	    {
		  "key_id_path": "%s",
		  "secret_path": "%s",
		},
	],
    "encryption_key": {
      "key_id_path": "%s",
      "secret_path": "%s",
    },
  }
}`

	signingKeyID, signingKeyIDCloser := mustWriteTempFile("signing-key-id")
	defer signingKeyIDCloser()

	signingKeySecret, signingKeySecretCloser := mustWriteTempFile("signing-key-secret")
	defer signingKeySecretCloser()

	verifyingKeyID, verifyingKeyIDCloser := mustWriteTempFile("verifying-key-id")
	defer verifyingKeyIDCloser()

	verifyingKeySecret, verifyingKeySecretCloser := mustWriteTempFile("verifying-key-secret")
	defer verifyingKeySecretCloser()

	verifyingKeyID2, verifyingKeyIDCloser2 := mustWriteTempFile("verifying-key-id2")
	defer verifyingKeyIDCloser2()

	verifyingKeySecret2, verifyingKeySecretCloser2 := mustWriteTempFile("verifying-key-secret2")
	defer verifyingKeySecretCloser2()

	encryptionKeyID, encryptionKeyIDCloser := mustWriteTempFile("encryption-key-id")
	defer encryptionKeyIDCloser()

	encryptionKeySecret, encryptionKeySecretCloser := mustWriteTempFile("encryption-key-secret")
	defer encryptionKeySecretCloser()

	configRAW := fmt.Sprintf(configTemplate,
		signingKeyID.Name(),
		signingKeySecret.Name(),
		verifyingKeyID.Name(),
		verifyingKeySecret.Name(),
		verifyingKeyID2.Name(),
		verifyingKeySecret2.Name(),
		encryptionKeyID.Name(),
		encryptionKeySecret.Name())

	f, cfgCloser := mustWriteTempFile(configRAW)
	defer cfgCloser()

	gotCfg := NewConfig()
	err := gotCfg.ParseFromFile(f.Name())
	require.NoError(t, err)

	assert.Equal(t, "signing-key-id", gotCfg.Visa.SigningKey.KeyID)
	assert.Equal(t, "signing-key-secret", gotCfg.Visa.SigningKey.Secret)
	assert.Equal(t, "verifying-key-id", gotCfg.Visa.VerifyingKeys[0].KeyID)
	assert.Equal(t, "verifying-key-secret", gotCfg.Visa.VerifyingKeys[0].Secret)
	assert.Equal(t, "verifying-key-id2", gotCfg.Visa.VerifyingKeys[1].KeyID)
	assert.Equal(t, "verifying-key-secret2", gotCfg.Visa.VerifyingKeys[1].Secret)
	assert.Equal(t, "encryption-key-id", gotCfg.Visa.EncryptionKey.KeyID)
	assert.Equal(t, "encryption-key-secret", gotCfg.Visa.EncryptionKey.Secret)
}
