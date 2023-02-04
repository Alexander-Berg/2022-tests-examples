package accounts

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestClient_InitLock(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	accountType := "some-account-type"
	timeout := 10
	uid := "a7432b5f-62b0-48b9-8159-e3e9899a001c"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/locks/init"
	rawResponse := StringMap{"status": StatusOK, "data": StringMap{"UID": uid}}

	m := makeClient(t, wantURL, rawResponse)

	accountLocation := entities.LocationAttributes{Type: accountType,
		Attributes: map[string]*string{"contract_id": &contractID}}
	got, err := m.InitLock(context.Background(), &accountLocation, timeout)
	assert.NoError(t, err)

	want := uid
	assert.EqualValues(t, want, *got)
	fmt.Printf("%s result: %v\n", t.Name(), *got)
}

func TestClient_PingLock(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	accountType := "some-account-type"
	timeout := 10
	uid := "a7432b5f-62b0-48b9-8159-e3e9899a001c"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/locks/ping"
	rawResponse := StringMap{"status": StatusOK, "data": nil}

	m := makeClient(t, wantURL, rawResponse)

	accountLocation := entities.LocationAttributes{Type: accountType,
		Attributes: map[string]*string{"contract_id": &contractID}}
	err := m.PingLock(context.Background(), &accountLocation, uid, timeout)
	assert.NoError(t, err)
	fmt.Printf("%s result: %v\n", t.Name(), nil)
}

func TestClient_GetLock(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	accountNamespace := "some-account-type"
	accountType := "some-account-type"
	uid := "a7432b5f-62b0-48b9-8159-e3e9899a001c"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/locks/state"
	wantParams := map[string]string{
		"contract_id": contractID,
		"type":        accountType,
		"namespace":   accountNamespace,
	}
	rawReponse := StringMap{"status": StatusOK, "data": StringMap{"UID": uid, "Locked": true}}

	m := makeClientX(t, wantURL, wantParams, rawReponse)

	accountLocation := entities.LocationAttributes{
		Namespace:  accountNamespace,
		Type:       accountType,
		Attributes: map[string]*string{"contract_id": &contractID},
	}
	got, err := m.GetLock(context.Background(), &accountLocation)
	assert.NoError(t, err)
	want := entities.LockInfo{UID: uid, Locked: true}
	assert.EqualValues(t, want, *got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}

func TestClient_RemoveLock(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	accountType := "some-account-type"
	uid := "a7432b5f-62b0-48b9-8159-e3e9899a001c"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/locks/remove"
	rawReponse := StringMap{"status": StatusOK, "data": nil}

	m := makeClient(t, wantURL, rawReponse)

	accountLocation := entities.LocationAttributes{Type: accountType,
		Attributes: map[string]*string{"contract_id": &contractID}}
	err := m.RemoveLock(context.Background(), &accountLocation, uid)
	assert.NoError(t, err)
	fmt.Printf("%s result: %v\n", t.Name(), nil)
}
