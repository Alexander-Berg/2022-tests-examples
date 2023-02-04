package paymenttoken

import (
	"encoding/base64"
	"fmt"
	"strconv"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

const (
	senderID    = "Yandex"
	recipientID = "someRecipient"
)

var (
	tomorrow                      = strconv.FormatInt(time.Now().Add(24*time.Hour).UnixNano()/int64(time.Millisecond), 10)
	googleVerifyingPublicKeysJSON = fmt.Sprintf(`{
		  "keys": [
			{
			  "keyValue": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM43yRydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw==",
			  "keyExpiration": "%s",
			  "protocolVersion": "ECv2"
			}
		  ]
		}`, tomorrow)
)

var (
	googleSigningEcV2IntermediatePrivateKeyPkcs8Base64 = mustParsePKCS8PrivateKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKevaKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAzjfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL")

	googleSigningEcV2PrivateKeyPkcs8Base64 = mustParsePKCS8PrivateKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgKvEdSS8f0mjTCNKevaKXIzfNC5b4A104gJWI9TsLIMqhRANCAAT/X7ccFVJt2/6Ps1oCt2AzKhIAzjfJHJ3Op2DVPGh1LMD3oOPgxzUSIqujHG6dq9Ui93Eacl4VWJPMW/MVHHIL")

	googleSigningEcV2IntermediatePublicKeyX509Base64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE/1+3HBVSbdv+j7NaArdgMyoSAM43yRydzqdg1TxodSzA96Dj4Mc1EiKroxxunavVIvdxGnJeFViTzFvzFRxyCw=="

	merchantPublicKeyBase64 = mustParseRawUncompressedPublicKey("BOdoXP+9Aq473SnGwg3JU1aiNpsd9vH2ognq4PtDtlLGa3Kj8TPf+jaQNPyDSkh3JUhiS0KyrrlWhAgNZKHYF2Y=")

	merchantPrivateKeyPkcs8Base64 = mustParsePKCS8PrivateKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgCPSuFr4iSIaQprjjchHPyDu2NXFe0vDBoTpPkYaK9dehRANCAATnaFz/vQKuO90pxsINyVNWojabHfbx9qIJ6uD7Q7ZSxmtyo/Ez3/o2kDT8g0pIdyVIYktCsq65VoQIDWSh2Bdm")
)

func TestEcV2WithPrecomputedKeys(t *testing.T) {
	sentMsg := &EncryptedMessage{
		PaymentMethod: PaymentMethodCard,
		PaymentMethodDetails: PaymentMethodDetails{
			AuthMethod: AuthMethodCloudToken,
			PAN:        "123",
			Cryptogram: "123",
		},
	}

	cert, err := makeCert(ecV2, googleSigningEcV2PrivateKeyPkcs8Base64, googleSigningEcV2IntermediatePublicKeyX509Base64)
	require.NoError(t, err)

	sender, err := NewSender(googleSigningEcV2IntermediatePrivateKeyPkcs8Base64, cert, WithSenderID(senderID))
	require.NoError(t, err)

	token, err := sender.SealJSONMessage(recipientID, sentMsg, merchantPublicKeyBase64)
	require.NoError(t, err)

	verifyingKeys, err := ParseSenderVerifyingKeys(googleVerifyingPublicKeysJSON)
	require.NoError(t, err)

	recipientPrivateKey := merchantPrivateKeyPkcs8Base64
	require.NoError(t, err)
	client := NewRecipient(
		recipientID,
		verifyingKeys,
		WithFromSenderID(senderID),
		WithPrivateKey(recipientPrivateKey),
	)
	require.NoError(t, err)

	recvMsg := &EncryptedMessage{}
	err = client.UnsealJSONMessage(token, recvMsg)
	require.NoError(t, err)
	require.Equal(t, sentMsg, recvMsg, "msg must be decoded")
}

func TestEcV2Async(t *testing.T) {
	sender, recipient := mustCreateECv2Clients()
	const goroutines = 1024

	var wg sync.WaitGroup

	worker := func() {
		defer wg.Done()
		data := randBytes(1000)
		token, err := sender.Seal(recipientID, data, merchantPublicKeyBase64)
		require.NoError(t, err)

		decoded, err := recipient.Unseal(token)
		require.NoError(t, err)
		require.Equal(t, data, decoded)
	}

	for i := 0; i < goroutines; i++ {
		wg.Add(1)
		go worker()
	}

	wg.Wait()
}

func TestEcV2MustFailOnCorruptedContent(t *testing.T) {
	msgText := []byte("this_is_the_message")

	sender, recipient := mustCreateECv2Clients()

	token, err := sender.Seal(recipientID, msgText, merchantPublicKeyBase64)
	require.NoError(t, err)

	actual := token.SignedMessage
	token.SignedMessage = actual + "_not"

	_, err = recipient.Unseal(token)
	require.Errorf(t, err, "data is corrupted")

	token.SignedMessage = actual
	signature, _ := base64.StdEncoding.DecodeString(token.Signature)
	signature[0] = signature[0] + 1
	token.Signature = base64.StdEncoding.EncodeToString(signature)

	_, err = recipient.Unseal(token)
	require.Errorf(t, err, "signature is corrupted")
}

func TestCanCreateValidIntermediateSigningKey(t *testing.T) {
	cert, err := makeCert(ecV2, googleSigningEcV2PrivateKeyPkcs8Base64, googleSigningEcV2IntermediatePublicKeyX509Base64)
	require.NoError(t, err)

	providers, err := ParseSenderVerifyingKeys(googleVerifyingPublicKeysJSON)
	require.NoError(t, err)

	_, err = cert.validateAndGetSigningKey(senderID, ecV2.protocolVersion, providers)
	require.NoError(t, err)
}
