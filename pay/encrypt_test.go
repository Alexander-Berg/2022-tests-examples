package pkcs7

import (
	"bytes"
	"crypto/x509"
	"encoding/pem"
	"io/ioutil"
	"os/exec"
	"path"
	"runtime"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/yatest"
)

func generateKeys(t *testing.T, keyPath, certPath string) *x509.Certificate {
	opensslCmd := mustOpensslCmd(t)
	opensslCnf := mustOpensslCnf(t)
	cmd := exec.Command(
		opensslCmd,
		"req",
		"-config", opensslCnf,
		"-newkey", "rsa:2048",
		"-nodes",
		"-keyout", keyPath,
		"-x509", "-days", "365",
		"-out", certPath,
		"-subj", "/CN=localhost",
	)
	if err := cmd.Run(); err != nil {
		t.Fatalf("failed to generate keys: %s", err.Error())
	}

	cert, err := ioutil.ReadFile(certPath)

	if err != nil {
		t.Fatalf("failed to read cert: %s", err.Error())
	}

	block, _ := pem.Decode(cert)

	if block == nil {
		t.Fatal("failed to parse PEM block containing the public key")
	}

	pub, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		t.Fatalf("failed to parse DER encoded public key: %s", err.Error())
	}

	return pub
}

func decryptMessage(t *testing.T, msg []byte, keyPath string) []byte {
	opensslCmd := mustOpensslCmd(t)
	cmd := exec.Command(
		opensslCmd, "smime", "-decrypt",
		"-inform", "der",
		"-inkey", keyPath,
	)
	var out bytes.Buffer
	var errOut bytes.Buffer
	cmd.Stdin = bytes.NewReader(msg)
	cmd.Stdout = &out
	cmd.Stderr = &errOut

	if err := cmd.Run(); err != nil {
		t.Fatalf("failed to decrypt message: %s", err.Error())
	}

	return out.Bytes()
}

func runOnUnix(t *testing.T) {
	if !(runtime.GOOS == "linux" || runtime.GOOS == "darwin") {
		t.Skip("only unix support")
	}
}

func mustOpensslCmd(t *testing.T) string {
	if !yatest.HasYaTestContext() {
		return "openssl"
	}

	cmd, err := yatest.BinaryPath("contrib/libs/openssl/apps/openssl")
	require.NoError(t, err, cmd)
	return cmd
}

func mustOpensslCnf(t *testing.T) string {
	return yatest.SourcePath("pay/duckgo/example-configs/openssl.cnf")
}

func TestEncrypt(t *testing.T) {
	runOnUnix(t)

	plaintext := []byte("Hello Secret World!")
	certPath := path.Join(t.TempDir(), "certificate.pem")
	keyPath := path.Join(t.TempDir(), "key.pem")

	cert := generateKeys(t, keyPath, certPath)

	encrypted, err := Encrypt(plaintext, []*x509.Certificate{cert})

	if err != nil {
		t.Fatal(err)
	}

	decrypted := decryptMessage(t, encrypted, keyPath)
	if !bytes.Equal(plaintext, decrypted) {
		t.Errorf("encrypted data does not match plaintext:\n\tExpected: %s\n\tActual: %s", plaintext, decrypted)
	}
}

func TestPad(t *testing.T) {
	tests := []struct {
		Original  []byte
		Expected  []byte
		BlockSize int
	}{
		{[]byte{0x1, 0x2, 0x3, 0x10}, []byte{0x1, 0x2, 0x3, 0x10, 0x4, 0x4, 0x4, 0x4}, 8},
		{[]byte{0x1, 0x2, 0x3, 0x0, 0x0, 0x0, 0x0, 0x0}, []byte{0x1, 0x2, 0x3, 0x0, 0x0, 0x0, 0x0, 0x0, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8, 0x8}, 8},
	}
	for _, test := range tests {
		padded, err := pad(test.Original, test.BlockSize)
		if err != nil {
			t.Errorf("pad encountered error: %s", err)
			continue
		}
		if !bytes.Equal(test.Expected, padded) {
			t.Errorf("pad results mismatch:\n\tExpected: %X\n\tActual: %X", test.Expected, padded)
		}
	}
}
