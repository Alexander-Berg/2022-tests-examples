package paymenttoken

import (
	"crypto/ecdsa"
	"crypto/rand"
	"encoding/base64"
	"time"

	"github.com/google/tink/go/hybrid/subtle"
)

func makeCert(config protocolVersionConfig, signingPrivateKey *ecdsa.PrivateKey, intermediateSigningKey string) (*IntermediateSigningCert, error) {
	expireTime := time.Now().Add(24 * time.Hour)
	certFactory, err := NewIntermediateSigningCertFactory(
		[]*ecdsa.PrivateKey{signingPrivateKey},
		config.protocolVersion,
		senderID,
		intermediateSigningKey,
		expireTime)
	if err != nil {
		return nil, err
	}

	signingCert, err := certFactory.CreateIntermediateSigningCert()
	if err != nil {
		return nil, err
	}

	return signingCert, nil
}

func mustParsePKCS8PrivateKey(base64Key string) *ecdsa.PrivateKey {
	key, err := ParsePKCS8PrivateKey(base64Key)
	if err != nil {
		panic(err)
	}
	return key
}

func mustParsePKIXPublicKey(base64Key string) *ecdsa.PublicKey {
	key, err := ParseB64ECPublicKey(base64Key)
	if err != nil {
		panic(err)
	}
	return key
}

func mustParseRawUncompressedPublicKey(base64Key string) *ecdsa.PublicKey {
	key, err := parseRawUncompressedPublicKey(base64Key)
	if err != nil {
		panic("invalid key")
	}
	return key
}

// parseRawUncompressedPublicKey - parse pub key from base64 encoded uncompressed point format. Curve NIST_P256 will be used!
func parseRawUncompressedPublicKey(pkBase64 string) (*ecdsa.PublicKey, error) {
	key, err := base64.StdEncoding.DecodeString(pkBase64)
	if err != nil {
		return nil, err
	}
	curveP256, _ := subtle.GetCurve("NIST_P256")
	point, err := subtle.PointDecode(curveP256, pointFormatUncompressed, key)
	if err != nil {
		return nil, err
	}
	return &ecdsa.PublicKey{
		Curve: curveP256,
		X:     point.X,
		Y:     point.Y,
	}, nil
}

func mustCreateECv2Clients() (*Sender, *Recipient) {
	cert, err := makeCert(ecV2, googleSigningEcV2PrivateKeyPkcs8Base64, googleSigningEcV2IntermediatePublicKeyX509Base64)
	if err != nil {
		panic(err)
	}

	sender, err := NewSender(googleSigningEcV2IntermediatePrivateKeyPkcs8Base64, cert, WithSenderID(senderID))
	if err != nil {
		panic(err)
	}

	verifyingKeys, err := ParseSenderVerifyingKeys(googleVerifyingPublicKeysJSON)
	if err != nil {
		panic(err)
	}

	recipient := NewRecipient(
		recipientID,
		verifyingKeys,
		WithFromSenderID(senderID),
		WithPrivateKey(merchantPrivateKeyPkcs8Base64),
	)

	return sender, recipient
}

func randBytes(n uint) []byte {
	bytes := make([]byte, n)
	if _, err := rand.Read(bytes); err != nil {
		panic(err)
	}
	return bytes
}
