package server

import (
	"bytes"
	"encoding/base64"
	"os/exec"
	"path"
	"runtime"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/yatest"
)

func generateKeys(t *testing.T, keyPath, certPath string) {
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
}

func decryptMessage(t *testing.T, msgBase64, keyPath string) string {
	opensslCmd := mustOpensslCmd(t)
	cmd := exec.Command(
		opensslCmd,
		"smime",
		"-decrypt",
		"-inform", "der",
		"-inkey", keyPath,
	)
	decoded, err := base64.StdEncoding.DecodeString(msgBase64)
	if err != nil {
		t.Fatalf("failed to decode message: %s", err.Error())
	}
	var out bytes.Buffer
	var errOut bytes.Buffer
	cmd.Stdin = bytes.NewReader(decoded)
	cmd.Stdout = &out
	cmd.Stderr = &errOut

	if err := cmd.Run(); err != nil {
		t.Fatalf("failed to decrypt message: %s", err.Error())
	}

	return out.String()
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

func TestEncryptCMS(t *testing.T) {
	runOnUnix(t)

	certPath := path.Join(t.TempDir(), "certificate.pem")
	keyPath := path.Join(t.TempDir(), "key.pem")

	generateKeys(t, keyPath, certPath)

	cert, err := loadCertificate(certPath)

	if err != nil {
		t.Fatalf("failed to read cert: %s", err.Error())
	}

	encrypted, err := EncryptCMS([]byte("hello world"), cert)

	if err != nil {
		t.Fatalf("failed to encrypt data: %s", err.Error())
	}

	decrypted := decryptMessage(t, encrypted, keyPath)

	if decrypted != "hello world" {
		t.Errorf("expected %s, given %s", "hello world", decrypted)
	}
}
