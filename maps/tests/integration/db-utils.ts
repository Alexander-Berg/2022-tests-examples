import {dbClient} from '../../app/lib/db-client';
import {IntervalInfo} from '../../app/providers/zones';
import {AppId, Provider} from '../../app/types';

export async function cleanTestDb(): Promise<void> {
    await dbClient.executeWriteQuery({
        text: `SELECT truncate_tables()`
    });
}

export async function insertZone(args: {
    provider: string,
    providerParkingId: string,
    intervals: IntervalInfo[],
    isRaisedTariff?: boolean
}) {
    await dbClient.executeWriteQuery({
        text: `INSERT INTO zones
            (provider, provider_parking_id, intervals, attributes) VALUES
            ($1, $2, $3, $4)`,
        values: [
            args.provider,
            args.providerParkingId,
            JSON.stringify(args.intervals),
            {isRaisedTariff: Boolean(args.isRaisedTariff)}
        ]
    });
}

export type SessionData = {
    uid: string;
    phone: string;
    active: boolean;
    vehiclePlate: string;
    provider: Provider;
    providerParkingId: string;
    providerSessionId: string;
    appId: AppId;
    publicId: string;
};

export type Timeframe = {
    start: Date;
    end: Date;
    cost: string;
};

export async function insertSession(session: SessionData, timeframes?: Timeframe[]) {
    const {data: {rows: [{id: sessionId}]}} = await dbClient.executeWriteQuery<{id: string}>({
        text: `INSERT INTO sessions
            (
                public_id,
                user_uid,
                user_phone,
                active,
                vehicle_plate,
                provider,
                provider_parking_id,
                provider_session_id,
                app_id
            ) VALUES
            (
                $1,
                $2,
                $3,
                $4,
                $5,
                $6,
                $7,
                $8,
                $9
            )
            RETURNING id`,
        values: [
            session.publicId,
            session.uid,
            session.phone,
            session.active,
            session.vehiclePlate,
            session.provider,
            session.providerParkingId,
            session.providerSessionId,
            session.appId
        ]
    });

    if (timeframes && timeframes.length) {
        for (const timeframe of timeframes) {
            await dbClient.executeWriteQuery({
                text: `INSERT INTO timeframes
                    (session_id, timestamp_start, timestamp_end, cost) VALUES
                    ($1, $2, $3, $4)`,
                values: [
                    sessionId,
                    timeframe.start,
                    timeframe.end,
                    timeframe.cost
                ]
            });
        }
    }
}
