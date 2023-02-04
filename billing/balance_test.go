package accounts

import (
	"context"
	"fmt"
	"strconv"
	"testing"

	"github.com/shopspring/decimal"
	"github.com/stretchr/testify/assert"
)

func TestClient_GetBalance(t *testing.T) {
	// Исходные договор и время
	contractID := 128699879345
	now := int64(1608637373949006416)
	accountNamespace := "some-account-namespace"
	accountType := "some-account-type"
	Debit, Credit := "50", "100.77"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/accounts/balance"
	wantParams := map[string]string{
		"contract_id": strconv.Itoa(contractID),
		"dt":          strconv.FormatInt(now, 10),
		"type":        accountType,
		"namespace":   accountNamespace,
	}

	var tests = []struct {
		rawResponse any
		want        decimal.Decimal
	}{
		{StringMap{"status": StatusOK, "data": []StringMap{{"loc": StringMap{"type": accountType,
			"contract_id": strconv.Itoa(contractID), "namespace": accountNamespace},
			"debit": Debit, "credit": Credit}}},
			decimal.RequireFromString(Credit).Sub(decimal.RequireFromString(Debit))},
		{[]byte(`{"status": "ok", "data": []}`), decimal.Zero},
		{[]byte(`{"status": "ok", "data": null}`), decimal.Zero},
	}

	for _, test := range tests {
		m := makeClientX(t, wantURL, wantParams, test.rawResponse)

		got, err := m.GetBalance(
			context.Background(),
			accountNamespace,
			accountType,
			StringMap{"contract_id": contractID},
			&now,
		)
		assert.NoError(t, err)

		assert.EqualValues(t, test.want, got)
		fmt.Printf("%s result: %v\n", t.Name(), got)
	}
}
