/* eslint-disable no-console */

import { ArcanumApiEndpoint, configureApiController, generated } from '@yandex-int/si.ci.arcanum-api';
// import assert from 'assert';
// import {GetChangelistResponse} from "@yandex-int/si.ci.arcanum-api/build/generated/model/getChangelistResponse";

async function checkPr() {
    // const arcanumVcsClient = configureApiController(generated.UnsafeDiffsApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);
    // const arcanumVcsClient1 = configureApiController(generated.UnsafeApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);
    const UnsafeReviewRequestsApi = configureApiController(generated.UnsafeReviewRequestsApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);
    const UnsafeDiffsApi = configureApiController(generated.UnsafeDiffsApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);
    // const UnsafeApi = configureApiController(generated.UnsafeApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);
    // const UnsafeVcsApi = configureApiController(generated.UnsafeVcsApi, ArcanumApiEndpoint.Production, process.env.ARC_TOKEN);

    const res = await UnsafeReviewRequestsApi.getReviewRequest(2529499, 'active_diff_set,diff_sets,merge_commits');
    // console.log(res.body.data);

    const activeDiffSet = res.body.data?.active_diff_set;
    if (!activeDiffSet || !activeDiffSet.id) {
        return;
    }
    const res1 = await UnsafeDiffsApi.getDiffSet(2529499, activeDiffSet.id);
    console.log('diffSet.id', activeDiffSet.id, res1.body.data);

    /*const diffSets = res.body.data && res.body.data.diff_sets;
    (diffSets || []).forEach(async(diffSet) => {
        if (!diffSet.id) {
            return;
        }
        const res = await UnsafeDiffsApi.getDiffSet(2529499, diffSet.id, 'description,patch_url,patch_stats');
        console.log('diffSet.id', res.body.data);
    })*/
}

checkPr();
