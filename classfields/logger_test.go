package logs

import (
	"bytes"
	"encoding/json"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestLevels(t *testing.T) {
	buffer := bytes.Buffer{}
	logger := New(&buffer, "", "debug", "", "", "")
	m := make(map[string]interface{})

	buffer.Reset()
	logger.Info("123")
	require.NoError(t, json.Unmarshal(buffer.Bytes(), &m))
	require.Equal(t, "123", m["_message"])
	require.Equal(t, "INFO", m["_level"])

	buffer.Reset()
	logger.Warn("123")
	require.NoError(t, json.Unmarshal(buffer.Bytes(), &m))
	require.Equal(t, "123", m["_message"])
	require.Equal(t, "WARN", m["_level"])

	buffer.Reset()
	logger.Error("123")
	require.NoError(t, json.Unmarshal(buffer.Bytes(), &m))
	require.Equal(t, "123", m["_message"])
	require.Equal(t, "ERROR", m["_level"])

	buffer.Reset()
	logger.Debug("123")
	require.NoError(t, json.Unmarshal(buffer.Bytes(), &m))
	require.Equal(t, "123", m["_message"])
	require.Equal(t, "DEBUG", m["_level"])
}
