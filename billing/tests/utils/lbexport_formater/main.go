package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"time"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

// InputEvent for the following json: {
//   "account_id":12,"account_type":"payout","amount":"10","info":null,
//   "type":"credit","client_id":"2","dt":1627465336,"batch_info":"{\"t\":1}"
// }
type InputEvent struct {
	ID          int64  `json:"id"`
	SeqID       int64  `json:"seq_id"`
	AccountID   int64  `json:"account_id"`
	AccountType string `json:"account_type"`
	Amount      string `json:"amount"`
	Info        string `json:"info"`
	Type        string `json:"type"`
	ClientID    string `json:"client_id"`
	ContractID  string `json:"contract_id"`
	Currency    string `json:"currency"`
	Namespace   string `json:"namespace"`
	Dt          *int64 `json:"dt"`
	BatchID     int64  `json:"batch_id"`
	BatchType   string `json:"batch_type"`
	BatchExtID  string `json:"batch_ext_id"`
	BatchInfo   string `json:"batch_info"`
	BatchCount  int32  `json:"batch_count"`
}

func main() {
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		var (
			event     InputEvent
			entries   []entities.LbExportEntry
			batchInfo []byte
			info      []byte
		)
		data := scanner.Bytes()
		if err := json.Unmarshal(data, &event); err != nil {
			fmt.Println(err)
			continue
		}
		dt := time.Now().Add(-1 * time.Minute).UTC()
		if event.Dt != nil {
			dt = time.Unix(*event.Dt, 0)
		}
		if event.BatchInfo != "" {
			batchInfo = []byte(event.BatchInfo)
		}
		if event.Info != "" {
			info = []byte(event.Info)
		}
		entries = append(
			entries,
			entities.LbExportEntry{
				ID:    event.ID,
				SeqID: event.SeqID,
				Event: entities.ExportEvent{
					EventAttributes: entities.EventAttributes{
						Loc: entities.LocationAttributes{
							Type:      event.AccountType,
							Namespace: event.Namespace,
							Attributes: map[string]*string{
								"client_id":   &event.ClientID,
								"contract_id": &event.ContractID,
								"currency":    &event.Currency,
							},
						},
						Type:   event.Type,
						Dt:     dt,
						Amount: event.Amount,
						Info:   info,
					},
					AccountID: event.AccountID,
				},
				EventBatch: entities.ExportEventBatchMeta{
					ID: event.BatchID,
					EventBatch: entities.EventBatch{
						Type:       event.BatchType,
						Dt:         dt,
						ExternalID: event.BatchExtID,
						Info:       batchInfo,
						EventCount: event.BatchCount,
					},
				},
			},
		)
		exported, err := entities.NewLbExportBatch(entries, time.Now()).GetDataJSON()
		if err != nil {
			fmt.Println(err)
			continue
		}
		fmt.Println(string(exported[0]))
	}
	if err := scanner.Err(); err != nil {
		fmt.Fprintln(os.Stderr, "reading standard input:", err)
	}
}
