package accounts

import (
	"context"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestClient_GetAccountBalance(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	now := int64(1608637373949006416)
	accountNamespace := "some-account-namespace"
	accountType := "some-account-type"
	Debit, Credit := "50", "100.77"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/accounts/balance"
	wantParams := map[string]string{
		"contract_id": contractID,
		"dt":          strconv.FormatInt(now, 10),
		"type":        accountType,
		"namespace":   accountNamespace,
	}
	rawResponse := StringMap{
		"status": StatusOK,
		"data": []StringMap{{
			"loc": StringMap{
				"namespace":   accountNamespace,
				"type":        accountType,
				"contract_id": contractID,
			},
			"debit":  Debit,
			"credit": Credit,
		}}}

	m := makeClientX(t, wantURL, wantParams, rawResponse)

	accountLocation := entities.LocationAttributes{
		Type:       accountType,
		Namespace:  accountNamespace,
		Attributes: map[string]*string{"contract_id": &contractID},
	}
	got, err := m.GetAccountBalance(context.Background(), &accountLocation, &now)
	assert.NoError(t, err)

	want := []entities.BalanceAttributes{{
		Loc: entities.LocationAttributes{
			Namespace:  accountNamespace,
			Type:       accountType,
			Attributes: map[string]*string{"contract_id": &contractID},
		},
		Debit:  Debit,
		Credit: Credit,
	}}
	assert.EqualValues(t, want, got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}

func TestClient_GetAccountTurnover(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	dtFrom := int64(1608637373949006416)
	dtTo := int64(1608637373949006416) + 666
	accountNamespace := "some-account-namespace"
	accountType := "some-account-type"
	Debit, Credit := "50", "100.77"

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/accounts/turnover"
	wantParams := map[string]string{
		"contract_id": contractID,
		"dt_from":     strconv.FormatInt(dtFrom, 10),
		"dt_to":       strconv.FormatInt(dtTo, 10),
		"type":        accountType,
		"namespace":   accountNamespace,
	}
	rawResponse := StringMap{
		"status": StatusOK,
		"data": []StringMap{{
			"loc":             StringMap{"namespace": accountNamespace, "type": accountType, "contract_id": contractID},
			"debit_init":      Debit,
			"credit_init":     Credit,
			"debit_turnover":  Debit,
			"credit_turnover": Credit,
		}}}

	m := makeClientX(t, wantURL, wantParams, rawResponse)

	accountLocation := entities.LocationAttributes{
		Namespace:  accountNamespace,
		Type:       accountType,
		Attributes: map[string]*string{"contract_id": &contractID},
	}
	got, err := m.GetAccountTurnover(context.Background(), &accountLocation, dtFrom, dtTo)
	assert.NoError(t, err)

	want := []entities.TurnoverAttributes{{
		Loc: entities.LocationAttributes{
			Namespace:  accountNamespace,
			Type:       accountType,
			Attributes: map[string]*string{"contract_id": &contractID},
		},
		DebitInit:      Debit,
		CreditInit:     Credit,
		DebitTurnover:  Debit,
		CreditTurnover: Credit,
	}}
	assert.EqualValues(t, want, got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}

func TestClient_GetAccountDetailedTurnover(t *testing.T) {
	// Исходные договор и время
	contractID := "128699879345"
	dtFrom := int64(1608637373949006416)
	dtTo := int64(1608637373949006416) + 666
	accountNamespace := "some-account-namespace"
	accountType := "some-account-type"
	Debit, Credit := "50", "100.77"
	timeE := time.Unix(1608091866, 0).In(time.UTC)

	// Адрес, по которому будем в систему счетов ходить
	wantURL := testVersionPrefix + "/accounts/turnover/detailed"
	wantParams := map[string]string{
		"contract_id": contractID,
		"dt_from":     strconv.FormatInt(dtFrom, 10),
		"dt_to":       strconv.FormatInt(dtTo, 10),
		"type":        accountType,
		"namespace":   accountNamespace,
	}
	rawResponse := StringMap{
		"status": StatusOK,
		"data": []StringMap{{
			"loc":             StringMap{"namespace": accountNamespace, "type": accountType, "contract_id": contractID},
			"debit_init":      Debit,
			"credit_init":     Credit,
			"debit_turnover":  Debit,
			"credit_turnover": Credit,
			"events": []StringMap{{
				"type":       "666",
				"dt":         entities.APIDt(timeE),
				"info":       "6666666666",
				"amount":     "666",
				"event_type": "666",
				"event_id":   "666",
			}}}}}
	//NOTE: check this "dt": timeE.Format("2006-01-02T15:04:05Z07:00")

	m := makeClientX(t, wantURL, wantParams, rawResponse)

	accountLocation := entities.LocationAttributes{
		Namespace:  accountNamespace,
		Type:       accountType,
		Attributes: map[string]*string{"contract_id": &contractID},
	}
	got, err := m.GetAccountDetailedTurnover(context.Background(), &accountLocation, dtFrom, dtTo)
	assert.NoError(t, err)

	want := []entities.DetailedTurnoverAttributes{{
		Loc: entities.LocationAttributes{
			Namespace:  accountNamespace,
			Type:       accountType,
			Attributes: map[string]*string{"contract_id": &contractID},
		},
		DebitInit:      Debit,
		CreditInit:     Credit,
		DebitTurnover:  Debit,
		CreditTurnover: Credit,
		Events: []entities.EventDetails{{Type: "666",
			Dt:     timeE,
			Info:   []byte(`"6666666666"`),
			Amount: "666", EventType: "666", EventID: "666"}},
	}}
	assert.EqualValues(t, want, got)
	fmt.Printf("%s result: %v\n", t.Name(), got)
}
