package logbroker

import (
	"context"
	"errors"
	"testing"

	"github.com/stretchr/testify/require"
)

// TestFormatOebsErrorMessage проверяет функцию формирования сообщения в ошибочный топик
func TestFormatOebsErrorMessage(t *testing.T) {
	ctx := context.Background()

	sourceData := []byte(`["id": "123"]`)
	data, err := FormatOebsErrorMessage(ctx, sourceData,
		errors.New("some error"), "oebs", UnknownSystem, false)
	require.NoError(t, err)
	expected := "{\"source_message\":\"WyJpZCI6ICIxMjMiXQ==\",\"error\":\"some error\",\"source\":\"unknown system\",\"dry_run\":false,\"module\":\"oebs\",\"system\":\"billing\"}"
	require.Equal(t, expected, string(data))

	// Проверяем корректность, когда err = nil
	data, err = FormatOebsErrorMessage(ctx, sourceData,
		nil, "oebs", UnknownSystem, true)
	require.NoError(t, err)
	expected = "{\"source_message\":\"WyJpZCI6ICIxMjMiXQ==\",\"error\":\"\",\"source\":\"unknown system\",\"dry_run\":true,\"module\":\"oebs\",\"system\":\"billing\"}"
	require.Equal(t, expected, string(data))
}
