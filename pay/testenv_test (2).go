package server

import (
	"context"
	"crypto"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"fmt"
	"io/ioutil"
	mrand "math/rand"
	"os"
	"path"
	"testing"
	"time"

	"github.com/gofrs/uuid"
	zaplog "go.uber.org/zap"
	"gopkg.in/square/go-jose.v2"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	"a.yandex-team.ru/pay/duckgo/mastercard/mcmock"
	"a.yandex-team.ru/pay/duckgo/paymenttoken"
	"a.yandex-team.ru/pay/duckgo/visa"
	"a.yandex-team.ru/pay/duckgo/visa/visamock"
)

type testConfigOption interface {
	apply(config *Config)
}

type testConfigOptionFunc func(config *Config)

func (t testConfigOptionFunc) apply(config *Config) {
	t(config)
}

func withSameVisaVerificationAndSigningKey() testConfigOption {
	return testConfigOptionFunc(
		func(c *Config) {
			c.Visa.SigningKey = c.Visa.VerifyingKeys[0]
		})
}

type TestEnv struct {
	t *testing.T

	SRCServiceID string

	BaseCtx   context.Context
	cancelCtx context.CancelFunc

	mcServer   *mcmock.SRCServer
	visaServer *visamock.VTSServer
	Server     *Server
	Client     *Client

	visaRelationshipID string

	mcSigningKey    *testKeyFile
	mcEncryptionKey *testKeyFile

	recipientVerificationKey       *ecdsa.PrivateKey
	recipientVerificationPublicKey *testKeyFile
	recipientKeySigner             *paymenttoken.RecipientKeySigner

	ptRootCA *rootCA
}

func NewCustomTestEnv(t *testing.T, isVisaEnabled bool, options ...testConfigOption) (*TestEnv, error) {
	logger := mustCreateTestLogger()
	mcServer := mcmock.NewSRCServer(logger)

	thalesCertPath := path.Join(t.TempDir(), "certificate.pem")
	thalesKeyPath := path.Join(t.TempDir(), "key.pem")
	generateKeys(t, thalesKeyPath, thalesCertPath)

	mcSigningKeyFile := newTestPrivateKeyFile(t, generateRSA256Key(t))
	mcEncryptionKey := generateRSA256Key(t)
	mcEncryptionKeyFile := newTestPrivateKeyFile(t, mcEncryptionKey)

	serviceID := genUUID()
	mcServer.RegisterService(serviceID,
		&mcmock.Service{
			EncryptionKey: jose.JSONWebKey{
				Key:       mcEncryptionKey.Public(),
				KeyID:     "service-kid",
				Algorithm: "RSA-OAEP-256",
				Use:       "enc",
			},
		})

	ptRootCA := newRootCA(t)

	visaSigningKey := visa.SharedKey{
		KeyID:  "inbound-key",
		Secret: "inbound-secret",
	}

	visaVerifyingKey := visa.SharedKey{
		KeyID:  "outbound-key",
		Secret: "outbound-secret",
	}

	visaEncryptionKey := visa.SharedKey{
		KeyID:  "enc-key",
		Secret: "enc-secret",
	}

	visaRelID := fmt.Sprintf("%d-%d", mrand.Int(), mrand.Int())
	visaServer := visamock.NewVTSServer(logger,
		visamock.WithEncryptionKey(&visaEncryptionKey),
		visamock.WithSigningKey(&visaSigningKey),
		visamock.WithEnrollAnyCard(),
		visamock.WithAllowedRelationshipID(visaRelID),
	)

	recipientVerificationKey := generateECP256Key(t)
	recipientVerificationPublicKey := newTestPublicKeyFile(t, recipientVerificationKey.Public())

	config := &Config{
		InternalAPI: ConfigInternalAPI{
			ListenAddr: "localhost:",
			Auth:       ConfigAuthSharedKey,
			SharedKey: ConfigSharedKey{
				SharedKey: "internal_api_shared_secret",
			},
		},
		ExternalAPI: ConfigExternalAPI{
			ListenAddr: "localhost:",
			DisableTLS: true,
			Auth:       ConfigAuthSharedKey,
			SharedKey: ConfigSharedKey{
				SharedKey: "external_api_shared_secret",
			},
		},
		Mastercard: ConfigMastercard{
			APIHostURL:              mcServer.GetBaseURL(),
			PublicKeysURL:           mcServer.GetKeysURL(),
			KeysUpdatePeriod:        "1s",
			KeysInitDownloadTimeout: "30s",
			ConsumerKey:             "mc_consumer_key",
			ClientID:                "mc_client_id",
			ServiceID:               serviceID,
			SigningKeyPath:          mcSigningKeyFile.Path(),
			EncryptionKeyPath:       mcEncryptionKeyFile.Path(),
		},
		Visa: ConfigVisa{
			IsEnabled:     isVisaEnabled,
			APIHostURL:    visaServer.GetBaseURL(),
			ClientAppID:   "Yandex.Pay",
			EnrollTimeout: "100s",
			SigningKey: ConfigNamedSharedKey{
				KeyID:  visaSigningKey.KeyID,
				Secret: visaSigningKey.Secret,
			},
			VerifyingKeys: []ConfigNamedSharedKey{
				{
					KeyID:  visaVerifyingKey.KeyID,
					Secret: visaVerifyingKey.Secret,
				},
			},
			EncryptionKey: ConfigNamedSharedKey{
				KeyID:  visaEncryptionKey.KeyID,
				Secret: visaEncryptionKey.Secret,
			},
		},
		PaymentToken: ConfigPaymentToken{
			SigningKeyPath:       ptRootCA.signingKeyFile.Path(),
			IntermediateCertPath: ptRootCA.intermJSONFile,

			RecipientVerificationPublicKeys: []string{
				recipientVerificationPublicKey.Path(),
			},
		},
		Wallet: ConfigWallet{
			Thales: ConfigThales{
				CardEncryptionCertPath: thalesCertPath,
			},
		},
		Logger: ConfigLogger{
			Sink:  "stdout",
			Level: "debug",
		},
	}

	for _, option := range options {
		option.apply(config)
	}

	server, err := New(config)
	if err != nil {
		return nil, err
	}
	server.Start()

	client := NewClient(server, config)

	baseCtx, cancelCtx := context.WithCancel(context.Background())

	recipientKeySigner := paymenttoken.NewRecipientKeySigner(recipientVerificationKey)

	return &TestEnv{
		t: t,

		BaseCtx:   baseCtx,
		cancelCtx: cancelCtx,

		SRCServiceID: serviceID,

		mcServer:   mcServer,
		visaServer: visaServer,
		Server:     server,
		Client:     client,

		visaRelationshipID: visaRelID,

		mcEncryptionKey: mcEncryptionKeyFile,
		mcSigningKey:    mcSigningKeyFile,

		recipientVerificationKey:       recipientVerificationKey,
		recipientVerificationPublicKey: recipientVerificationPublicKey,
		recipientKeySigner:             recipientKeySigner,

		ptRootCA: ptRootCA,
	}, nil
}

func NewTestEnv(t *testing.T, options ...testConfigOption) *TestEnv {
	env, err := NewCustomTestEnv(t, true, options...)
	if err != nil {
		t.Fatal("Can't create server: ", err)
	}
	return env
}

func (te *TestEnv) Close() {
	te.cancelCtx()

	te.Server.Shutdown(context.TODO())
	te.mcServer.Close()

	te.mcEncryptionKey.Close()
	te.mcSigningKey.Close()

	te.ptRootCA.Close()
	te.recipientVerificationPublicKey.Close()
}

func (te *TestEnv) GetRootCAKeySet() *paymenttoken.SenderVerifyingKeySet {
	return te.ptRootCA.GetRootKeySet()
}

func (te *TestEnv) SignRecipientKey(key *ecdsa.PublicKey) string {
	return te.recipientKeySigner.Sign(key)
}

func (te *TestEnv) GetVisaAllowedRelationshipID() string {
	return te.visaRelationshipID
}

func mustCreateTestLogger() log.Logger {
	logger, err := zap.New(zaplog.NewDevelopmentConfig())
	if err != nil {
		panic(err)
	}
	return logger
}

type testKeyFile struct {
	key  interface{}
	path string
}

func newTestPrivateKeyFile(t *testing.T, key interface{}) *testKeyFile {
	f, err := ioutil.TempFile("", "duckgo-test-env-")
	if err != nil {
		t.Fatal("ioutil.TempFile:", err)
	}
	defer func() { _ = f.Close() }()

	pkcs8, err := x509.MarshalPKCS8PrivateKey(key)
	if err != nil {
		t.Fatal("x509.MarshalPKCS8PrivateKey:", err)
	}

	pemBlock := &pem.Block{
		Type:  "PRIVATE KEY",
		Bytes: pkcs8,
	}

	err = pem.Encode(f, pemBlock)
	if err != nil {
		t.Fatal("pem.Encode:", err)
	}

	return &testKeyFile{
		key:  key,
		path: f.Name(),
	}
}

func newTestPublicKeyFile(t *testing.T, key interface{}) *testKeyFile {
	f, err := ioutil.TempFile("", "duckgo-test-env-")
	if err != nil {
		t.Fatal("ioutil.TempFile:", err)
	}
	defer func() { _ = f.Close() }()

	pkix, err := x509.MarshalPKIXPublicKey(key)
	if err != nil {
		t.Fatal("x509.MarshalPKIXPublicKey:", err)
	}

	pemBlock := &pem.Block{
		Type:  "PUBLIC KEY",
		Bytes: pkix,
	}

	err = pem.Encode(f, pemBlock)
	if err != nil {
		t.Fatal("pem.Encode:", err)
	}

	return &testKeyFile{
		key:  key,
		path: f.Name(),
	}
}

func (tk *testKeyFile) Path() string {
	return tk.path
}

func (tk *testKeyFile) Close() {
	_ = os.Remove(tk.path)
}

func generateRSA256Key(t *testing.T) *rsa.PrivateKey {
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatal("rsa.GenerateKey:", err)
	}
	return key
}

func generateECP256Key(t *testing.T) *ecdsa.PrivateKey {
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		t.Fatal("ecdsa.GenerateKey:", err)
	}
	return key
}

type rootCA struct {
	rootKey        *ecdsa.PrivateKey
	signingKeyFile *testKeyFile
	intermJSONFile string
}

func newRootCA(t *testing.T) *rootCA {
	rootKey := generateECP256Key(t)
	intermKey := generateECP256Key(t)

	intermJSONFile := createIntermediateCert(t, rootKey, intermKey)

	return &rootCA{
		rootKey:        rootKey,
		signingKeyFile: newTestPrivateKeyFile(t, intermKey),
		intermJSONFile: intermJSONFile,
	}
}

func convertPublicKeyToDERBase64(t *testing.T, key crypto.PublicKey) string {
	pubKey, err := x509.MarshalPKIXPublicKey(key)
	if err != nil {
		t.Fatal("x509.MarshalPKIXPublicKey:", err)
	}
	return base64.StdEncoding.EncodeToString(pubKey)
}

func createIntermediateCert(t *testing.T, rootKey, intermKey *ecdsa.PrivateKey) (filePath string) {
	signingKey := convertPublicKeyToDERBase64(t, intermKey.Public())

	factory, err := paymenttoken.NewIntermediateSigningCertFactory(
		[]*ecdsa.PrivateKey{rootKey},
		paymenttoken.ProtocolVersionECv2,
		paymenttoken.Yandex,
		signingKey,
		time.Now().Add(24*time.Hour),
	)
	if err != nil {
		t.Fatal("NewIntermediateSigningCertFactory:", err)
	}

	cert, err := factory.CreateIntermediateSigningCert()
	if err != nil {
		t.Fatal("CreateIntermediateSigningCert:", err)
	}

	certBytes, err := json.Marshal(cert)
	if err != nil {
		t.Fatal("cert Marshal:", err)
	}

	file, err := ioutil.TempFile("", "duckgo-test-root-ca-")
	if err != nil {
		t.Fatal("TempFile:", err)
	}
	defer func() { _ = file.Close() }()

	_, err = file.Write(certBytes)
	if err != nil {
		t.Fatal("cert Write():", err)
	}

	return file.Name()
}

func (ca *rootCA) SigningKeyPath() string {
	return ca.signingKeyFile.Path()
}

func (ca *rootCA) SigningCertPath() string {
	return ca.intermJSONFile
}

func (ca *rootCA) Close() {
	ca.signingKeyFile.Close()
	_ = os.Remove(ca.intermJSONFile)
}

func (ca *rootCA) GetRootKeySet() *paymenttoken.SenderVerifyingKeySet {
	return paymenttoken.NewSenderVerifyingKeySet(&paymenttoken.SenderVerifyingKey{
		ProtocolVersion: paymenttoken.ProtocolVersionECv2,
		PublicKey:       &ca.rootKey.PublicKey,
		ExpirationTime:  time.Now().Add(time.Hour),
	})
}

func genUUID() string {
	id, err := uuid.NewV4()
	if err != nil {
		panic(err)
	}

	return id.String()
}
