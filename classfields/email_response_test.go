package proxy

import (
	"encoding/json"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestEmailResponseDecode(t *testing.T) {
	var v emailResponse
	err := json.Unmarshal([]byte(emaillResponseJson), &v)
	require.NoError(t, err)
}

var (
	emaillResponseJson = `{
  "to_email": "jain@example.com",
  "to_yandex_puid": "01234567890",
  "from_name": "Bob",
  "from_email": "bob@example.com",
  "cc": [
    {
      "email": "vertisMary@yandex-team.ru",
      "name": "Mary"
    },
    {
      "email": "john@example.com",
      "name": "John"
    }
  ],
  "bcc": [
    {
      "email": "tom@example.com",
      "name": "Tom"
    },
    {
      "email": "vertisAlice@yandex-team.ru",
      "name": "Alice"
    },
    {
      "email": "jan@example.com",
      "name": "Jan"
    }
  ],
  "to": [
    {
      "email": "vertisAlice@yandex-team.ru",
      "name": "Alice"
    },
    {
      "email": "bob@example.com",
      "name": "Bob"
    }
  ],
  "attachments": [
    {
      "data": "",
      "mime_type": "application/text",
      "filename": "a.txt"
    }
  ],
  "args": {
    "var": "val",
    "num": 42
  },
  "headers": {
    "one": "1",
    "two": "2",
    "three": "3"
  },
  "unknownextkey": "extendedvalue",
  "async": true
}
`
)
