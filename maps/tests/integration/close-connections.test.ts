import * as db from '../../app/lib/db';

after(async () => {
    await db.forceCloseConnection();
});
