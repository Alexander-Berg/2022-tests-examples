package accounts

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestClient_GetState(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	accountNamespace := "some-account-namespace"
	accountType := "some-account-type"
	state := []byte(`[{"some": "weird"}, "data"]`)

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/accounts/state"
	wantParams := map[string]string{
		"contract_id": contractID,
		"type":        accountType,
		"namespace":   accountNamespace,
	}
	rawResponse := StringMap{"status": StatusOK, "data": state}

	m := makeClientX(t, wantURL, wantParams, rawResponse)

	accountLocation := entities.LocationAttributes{
		Namespace:  accountNamespace,
		Type:       accountType,
		Attributes: map[string]*string{"contract_id": &contractID},
	}
	got, err := m.GetState(context.Background(), &accountLocation)
	assert.NoError(t, err)

	want := state
	assert.EqualValues(t, want, got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}
