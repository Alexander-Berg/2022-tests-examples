package paymenttoken

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func BenchmarkEcV2WithPrecomputedKeysEncoding(b *testing.B) {
	b.ReportAllocs()
	msgText := []byte("{'token':{'somekey':'somevalue', 'type':'card_visa'}, 'ayfield':'justvalue1231123}")

	sender, _ := mustCreateECv2Clients()
	_, err := sender.Seal(recipientID, msgText, merchantPublicKeyBase64)
	require.NoError(b, err)

	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		_, _ = sender.Seal(recipientID, msgText, merchantPublicKeyBase64)
	}
}

func BenchmarkEcV2WithPrecomputedKeysDecoding(b *testing.B) {
	b.ReportAllocs()
	msgText := []byte("{'tokren':{'somekey':'somevalue', 'type':'card_visa'}, 'ayfield':'justvalue1231123}")

	sender, recipient := mustCreateECv2Clients()

	token, err := sender.Seal(recipientID, msgText, merchantPublicKeyBase64)
	require.NoError(b, err)

	_, err = recipient.Unseal(token)
	require.NoError(b, err)

	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		_, _ = recipient.Unseal(token)
	}
}
