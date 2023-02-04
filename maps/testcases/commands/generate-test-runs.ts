import { createCmfData } from '../courier-manager/company';
import { notifyInStarTrack } from '../notifyInStarTrack';

import { addTestRuns, updateTestRuns } from '../testpalm/testpalm';
import { getUsers } from '../tus/tus';
import { ArgumentsT } from '../testpalm/model';

export const generateTestRuns = async (flags: ArgumentsT): Promise<void> => {
  try {
    const { version, testRuns } = await addTestRuns(flags);
    const usersByUuid = await getUsers(testRuns);
    const companiesByUuid = await createCmfData(testRuns);
    await updateTestRuns(testRuns, usersByUuid, companiesByUuid);
    await notifyInStarTrack(version, testRuns);

    process.exit(0);
  } catch (error) {
    console.info(error);
    process.exit(1);
  }
};
