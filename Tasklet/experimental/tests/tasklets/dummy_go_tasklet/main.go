package main

import (
	"context"
	"encoding/json"
	"log"
	"os"

	"a.yandex-team.ru/tasklet/api/v2"
	sdk "a.yandex-team.ru/tasklet/experimental/sdk/go/dummy"
)

func doTaskletJob(input *taskletv2.GenericBinary) taskletv2.GenericBinary {
	log.Println("Input payload: ", string(input.Payload))

	var obj interface{}
	if err := json.Unmarshal(input.Payload, &obj); err != nil {
		log.Fatalf("Failed to parse json: %v", err)
	}

	resultData := obj.(map[string]interface{})["result_data"].(string)

	return taskletv2.GenericBinary{Payload: []byte(resultData)}
}

func main() {
	executorAddress, inputFile, outputFile, _ := os.Args[1], os.Args[2], os.Args[3], os.Args[4]

	input := taskletv2.GenericBinary{}
	if err := sdk.ReadInput(&input, inputFile); err != nil {
		log.Fatalf("Failed to read input: %v", err)
	}

	taskletContext := sdk.MustGetContext()
	localService := sdk.MustGetExecutorServiceClient(executorAddress)

	if resp, err := localService.GetContext(context.Background(), &taskletv2.GetContextRequest{}); err != nil {
		log.Fatalf("Failed to get context via local service: %v", err)
	} else {
		idInContext := taskletContext.GetMeta().GetId()
		idInResp := resp.GetContext().GetMeta().GetId()
		if idInResp == "" || idInResp != idInContext {
			log.Fatalf("Invalid context: ID in context = %v, ID in service = %v", idInContext, idInResp)
		}
		log.Printf("ExecutionID = %v", idInResp)
	}

	output := doTaskletJob(&input)

	if err := sdk.WriteOutput(&output, outputFile); err != nil {
		log.Fatalf("Failed to save output: %v", err)
	}
	log.Println("Done")
}
