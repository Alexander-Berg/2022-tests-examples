package cpf

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestParseNewFormat(t *testing.T) {
	contractID := "1234"
	otherContractID := "1235"
	cpfID := "2141-1241-1"
	cpfID2 := "2141-1241-2"
	otherCpfID := "2141-1242-1"

	info := []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295553",
   "tariffer_payload": {
	"dry_run": false,
	"common_ts": 1616518860,
	"contract_states": {
  		"invoices":{
			"%s": [
				{"id": 1231124, "operation_type": "INSERT", "external_id": "%s", "amount": 200},
				{"id": 1231125, "operation_type": "INSERT", "external_id": "%s", "amount": 300}
			],
			"%s": [
				{"id": 1231126, "operation_type": "INSERT", "external_id": "%s", "amount": 400}
			]
		},
  		"nettings": {
		}
	}
  }
}`, contractID, cpfID, cpfID2, otherContractID, otherCpfID))

	parsed, err := ParseEventInfo(contractID, info)
	require.NoError(t, err)
	assert.False(t, parsed.DryRun)
	require.Len(t, parsed.Invoices, 2)
	assert.False(t, parsed.Invoices[0].DryRun)
}

func TestParseDefaultDryRun(t *testing.T) {
	contractID := "1234"
	cpfID := "2141-1241-1"

	info := []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295553",
   "tariffer_payload": {
	"common_ts": 1616518860,
	"contract_states": {
  		"invoices": {
			"%s": [
				{"id": 1231124, "operation_type": "INSERT", "external_id": "%s", "amount": 200}
			]
		},
  		"nettings": {
		}
	}
  }
}`, contractID, cpfID))

	parsed, err := ParseEventInfo(contractID, info)
	require.NoError(t, err)
	assert.True(t, parsed.DryRun)
	require.Len(t, parsed.Invoices, 1)
	assert.True(t, parsed.Invoices[0].DryRun)
}

func TestParseFirstNotEmptyInfo(t *testing.T) {
	contractID := "1234"
	cpfID := "2141-1241-1"
	cpfID2 := "2141-1241-2"

	info := []byte(fmt.Sprintf(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295553",
   "tariffer_payload": {
	"dry_run": false,
	"common_ts": 1616518860,
	"contract_states": {
  		"invoices":{
			"%s": [
				{"id": 1231124, "operation_type": "INSERT", "external_id": "%s", "amount": 200},
				{"id": 1231125, "operation_type": "INSERT", "external_id": "%s", "amount": 300}
			]
		}
	}
  }
}`, contractID, cpfID, cpfID2))

	parsed, err := ParseEventInfo(contractID, nil, []byte{}, info)
	require.NoError(t, err)
	assert.False(t, parsed.DryRun)
	require.Len(t, parsed.Invoices, 2)
	assert.False(t, parsed.Invoices[0].DryRun)
}

func TestParseInfoSourcesWithoutInvoices(t *testing.T) {
	contractID := "1234"

	info1 := []byte(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295553",
   "tariffer_payload": {
	"common_ts": 1616518860,
	"contract_states": {
	}
  }
}`)
	info2 := []byte(`{
  "client_id": 92698540,
  "event_time": "2021-03-23T17:01:00+00:00",
  "transaction_id": "A4DCAD91-FC74-4B03-B6DD-5ED397295554",
   "tariffer_payload": {
	"dry_run": false,
	"common_ts": 1616518860,
	"contract_states": {
	}
  }
}`)

	parsed, err := ParseEventInfo(contractID, info1, info2)
	require.NoError(t, err)
	assert.False(t, parsed.DryRun)
	require.Len(t, parsed.Invoices, 0)
}
