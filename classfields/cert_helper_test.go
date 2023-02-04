package tls_cert

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGenerateSelfSignedCertificate(t *testing.T) {
	crt, err := GenerateSelfSignedCertificate("test")
	require.NoError(t, err)
	require.True(t, len(crt.Certificate) > 0)
	require.NotEmpty(t, crt.PrivateKey)
}
