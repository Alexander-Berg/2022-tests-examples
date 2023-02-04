import {PartnerData} from 'app/v1/types/partner-data';
import {Feedback, FeedbackCode} from 'app/v1/types/feedback';
import {DBBookingEntity} from 'app/v1/types/booking';
import db, {knex} from 'app/lib/db-client';
import {v4 as uuid} from 'uuid';
import {createDBBookingEntityMock, MockDBBookingInput} from 'tests/create-db-booking-entity-mock';
import createNewFeedback from 'app/queries/create-new-feedback';
import {DBResourcesCacheEntity, DBServicesCacheEntity} from 'types/common';

async function insertDBServicesCacheMock(services: DBServicesCacheEntity): Promise<void> {
    await db.executeWriteQuery(({execute}) => execute(knex('services_cache').insert(services)));
}

async function insertDBResourcesCacheMock(resources: DBResourcesCacheEntity): Promise<void> {
    await db.executeWriteQuery(({execute}) =>
        execute(knex('resources_cache').insert(resources).onConflict('id').merge())
    );
}

async function insertDBBookingMock(data: MockDBBookingInput): Promise<DBBookingEntity> {
    const booking = createDBBookingEntityMock(data);
    await db.executeWriteQuery(({execute}) => execute(knex('bookings').insert(booking)));
    return booking;
}

async function getAllBookingsCount(): Promise<number> {
    const dbResult = await db.executeReadQuery(({execute}) => execute(knex<{count: string}>('bookings').count('*')));
    return Number(dbResult.rows[0].count);
}

async function getDBBookingById(bookingId: string): Promise<DBBookingEntity | undefined> {
    const dbResult = await db.executeReadQuery(({execute}) =>
        execute(knex('bookings').select('*').where({id: bookingId}))
    );
    return dbResult.rows[0];
}

async function deleteDBBookingById(bookingId: string): Promise<void> {
    await db.executeWriteQuery(({execute}) => execute(knex('bookings').where({id: bookingId}).delete()));
}

async function getDBPartnerById(partnerId: string): Promise<PartnerData | undefined> {
    const dbResult = await db.executeReadQuery(({execute}) =>
        execute(knex('partners').select('*').where({id: partnerId}))
    );
    return dbResult.rows[0];
}

async function getAllPartners(): Promise<PartnerData[]> {
    const dbResult = await db.executeReadQuery(({execute}) => execute(knex<PartnerData>('partners').select('*')));
    return dbResult.rows;
}

async function insertDBPartnerMock(name: string, active: boolean, endpoint: string): Promise<PartnerData> {
    const partner: PartnerData = {name, active, endpoint, id: uuid(), logo_url: '', link_url: '', legal_name: ''};
    await db.executeWriteQuery(({execute}) => execute(knex('partners').insert(partner)));
    return partner;
}

async function getDBAllBookings(): Promise<DBBookingEntity[] | undefined> {
    const dbResult = await db.executeReadQuery(({execute}) => execute(knex('bookings').select('*')));
    return dbResult.rows;
}

async function getDBFeedbackById(feedbackId: string): Promise<Feedback | undefined> {
    const dbResult = await db.executeReadQuery(({execute}) =>
        execute(knex('feedback').select('*').where({id: feedbackId}))
    );
    return dbResult.rows[0];
}

async function insertDBFeedbackMock(bookingId: string, code: FeedbackCode, comment?: string): Promise<Feedback> {
    const insertFeedback = createNewFeedback(bookingId, {comment: comment || null, code});
    const feedback = await db.executeWriteQuery(insertFeedback);
    return feedback;
}

async function setDynamicConfig(config: Record<string, string>): Promise<void> {
    const preparedValues = Object.entries(config).map(([key, value]) => ({feature_flag: key, value}));
    await db.executeWriteQuery(({execute}) =>
        execute(
            knex.raw(
                `INSERT INTO dynamic_config SELECT * FROM json_populate_recordset(NULL::dynamic_config, ?) ON CONFLICT (feature_flag) DO UPDATE SET value = excluded.value;`,
                [JSON.stringify(preparedValues)]
            )
        )
    );
}

export {
    insertDBBookingMock,
    getDBBookingById,
    deleteDBBookingById,
    getDBPartnerById,
    getAllBookingsCount,
    getAllPartners,
    getDBAllBookings,
    insertDBPartnerMock,
    getDBFeedbackById,
    insertDBFeedbackMock,
    setDynamicConfig,
    insertDBResourcesCacheMock,
    insertDBServicesCacheMock
};
