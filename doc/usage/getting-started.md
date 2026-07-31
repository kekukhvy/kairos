# Getting Started with Kairos

This guide walks you through the first steps of using Kairos from a client
service. You will register a destination, create a task, read it back, update
it, and delete it.

All examples use `curl`. The server runs on port `8080` by default.

---

## Prerequisites

1. Start the infrastructure with Docker Compose:

   ```bash
   docker compose up -d
   ```

2. Start the Kairos Engine (this owns and applies the database schema):

   ```bash
   ./gradlew :kairos-engine:run
   ```

   The engine writes a health marker file once migrations complete (default:
   `/tmp/kairos-engine/engine.health`). This is what orchestration uses to
   confirm the schema is ready before starting the API. The engine stays running
   after startup — it hosts the scheduler loop in later milestones.

3. In a new terminal, start the Kairos API:

   ```bash
   ./gradlew :kairos-api:run
   ```

   The API will fail fast if the database schema is not initialized (schema
   ownership moved to the engine).

4. A **destination** must exist before you can create tasks. Tasks reference a
   destination by `destinationId`, and the reference is enforced at the API
   level. Creating a task with a `destinationId` that does not exist returns
   `400 Bad Request`.

   Register one using the Destinations API (see Step 1 below).

---

## Step 1 — Register a destination

A destination tells Kairos where to deliver messages. Create one for the Kafka
topic that the booking service listens on:

```bash
curl -s -X POST http://localhost:8080/api/v1/destinations \
  -H "Content-Type: application/json" \
  -d '{
    "destinationId": "booking-kafka",
    "destinationType": "KAFKA",
    "config": {"topic": "bookings"}
  }'
```

Kairos responds with `201 Created`:

```json
{
  "destinationId": "booking-kafka",
  "destinationType": "KAFKA",
  "config": { "topic": "bookings" },
  "createdAt": "2026-06-24T10:00:00.000000Z"
}
```

The `destinationId` you chose (`"booking-kafka"`) is the value you will supply
in every task that should deliver to this destination.

---

## Step 2 — Create a task

A booking service wants Kairos to fire an event when a reservation hold window
expires. It creates a task that describes what to send and where to send it.

```http
POST /api/v1/tasks
Content-Type: application/json

{
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Cancels a reservation after the hold window expires",
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v1",
  "payload": {
    "bookingId": "abc-123",
    "reason": "hold_expired"
  },
  "timeoutMs": 5000,
  "supportsRetry": true
}
```

```bash
curl -s -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "service": "booking-service",
    "name": "expire-booking",
    "description": "Cancels a reservation after the hold window expires",
    "destinationId": "booking-kafka",
    "eventName": "booking.expire.v1",
    "payload": {"bookingId": "abc-123", "reason": "hold_expired"},
    "timeoutMs": 5000,
    "supportsRetry": true
  }'
```

Kairos responds with `201 Created` and the full task including its server-assigned `id`:

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Cancels a reservation after the hold window expires",
  "active": true,
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v1",
  "payload": { "bookingId": "abc-123", "reason": "hold_expired" },
  "timeoutMs": 5000,
  "supportsRetry": true,
  "createdAt": "2026-06-24T10:15:30.123456Z",
  "updatedAt": "2026-06-24T10:15:30.123456Z"
}
```

Store the `id` — you will need it for all subsequent operations on this task.

Fields you did not supply:
- `active` defaults to `true`
- `description` and `payload` are optional and can be omitted or set to `null`
- `supportsRetry` defaults to `false` if omitted

---

## Step 3 — Read the task back

```bash
curl -s http://localhost:8080/api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

Returns `200 OK` with the same `TaskResponse` body shown above.

---

## Step 4 — List tasks

```bash
curl -s "http://localhost:8080/api/v1/tasks"
```

Returns `200 OK` with a paginated list:

```json
{
  "items": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "service": "booking-service",
      "name": "expire-booking",
      ...
    }
  ],
  "limit": 20,
  "offset": 0,
  "hasNext": false
}
```

`hasNext` is `true` when there are more items beyond the current page. Use
`limit` and `offset` to page through results:

```bash
curl -s "http://localhost:8080/api/v1/tasks?limit=5&offset=0"
curl -s "http://localhost:8080/api/v1/tasks?limit=5&offset=5"
```

The `limit` field in the response always reflects the effective value after
normalization: absent or `<= 0` becomes `20`; values above `100` are capped
at `100`.

Deleted tasks are never included in the list.

---

## Step 5 — Update the task

PUT replaces the full editable field set. `service` is immutable and must not
be included in the request body.

```bash
curl -s -X PUT http://localhost:8080/api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "expire-booking",
    "description": "Updated description after retry policy change",
    "active": true,
    "destinationId": "booking-kafka",
    "eventName": "booking.expire.v2",
    "payload": {"bookingId": "abc-123", "reason": "hold_expired", "version": 2},
    "timeoutMs": 10000,
    "supportsRetry": true
  }'
```

Returns `200 OK` with the updated task. Notice `updatedAt` has advanced:

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Updated description after retry policy change",
  "active": true,
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v2",
  "payload": { "bookingId": "abc-123", "reason": "hold_expired", "version": 2 },
  "timeoutMs": 10000,
  "supportsRetry": true,
  "createdAt": "2026-06-24T10:15:30.123456Z",
  "updatedAt": "2026-06-24T10:22:05.654321Z"
}
```

---

## Step 6 — Schedule the task

A task defines *what* to deliver and *where*. A schedule defines *when*. Create
a `ONCE` schedule to fire the task at a specific future time:

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479/schedules" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ONCE",
    "label": "expire-hold-abc123",
    "runAt": "2026-07-10T14:00:00Z"
  }'
```

Kairos responds with `201 Created`:

```json
{
  "id": "a1b2c3d4-0000-0000-0000-111111111111",
  "taskId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "type": "ONCE",
  "label": "expire-hold-abc123",
  "runAt": "2026-07-10T14:00:00Z",
  "cronExpression": null,
  "intervalSeconds": null,
  "timezone": "UTC",
  "active": true,
  "createdAt": "2026-07-09T09:00:00Z",
  "updatedAt": "2026-07-09T09:00:00Z"
}
```

Other schedule types:

- **`CRON`** — fires on a cron expression: supply `cronExpression` (and
  optionally `timezone`, defaulting to `UTC`). Omit `runAt`.
- **`FIXED`** — fires repeatedly every N seconds: supply `intervalSeconds`
  (`1–86400`). Omit `runAt`.

You can pause a schedule without deleting it:

```bash
curl -s -X PATCH \
  "http://localhost:8080/api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111/pause"
```

And resume it again:

```bash
curl -s -X PATCH \
  "http://localhost:8080/api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111/resume"
```

Both return `200 OK` with the updated `ScheduleResponse` (the `active` field
reflects the new state). To permanently remove a schedule:

```bash
curl -s -X DELETE \
  "http://localhost:8080/api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111"
```

Returns `204 No Content`.

---

## Step 7 — Delete the task

```bash
curl -s -X DELETE http://localhost:8080/api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

Returns `204 No Content` with no body. The task is soft-deleted: it will no
longer appear in the list or be returned by GET. Any further GET or PUT on the
same `id` returns `404`. A second DELETE on the same `id` returns `409 Conflict`.

After deletion, the task's `(service, name)` pair is freed and can be reused: you
can create a new task in the same service with the same name. The uniqueness
constraint applies only to live tasks.

---

## Common errors

| What you did | Status | Error message |
|---|---|---|
| Omitted a required field (e.g. `name`) | `400` | Validation message naming the missing field. |
| Sent `timeoutMs: -1` | `400` | Validation message about `timeoutMs`. |
| Used a `destinationId` that does not exist | `400` | Validation message about the unknown destination. |
| Passed a non-UUID path param (tasks or schedules) | `400` | Message identifying the bad value. |
| Supplied an unrecognized `destinationType` or schedule `type` | `400` | Message identifying the unknown type. |
| Sent a `runAt` that is in the past (ONCE schedules) | `400` | Validation message about `runAt`. |
| Sent `intervalSeconds` outside `1–86400` (FIXED schedules) | `400` | Validation message about `intervalSeconds`. |
| Supplied an unknown IANA timezone (CRON schedules) | `400` | Validation message about `timezone`. |
| Task `id` does not exist or was deleted | `404` | Message identifying the task. |
| Destination `id` does not exist | `404` | Message identifying the destination. |
| Schedule `id` does not exist | `404` | Message identifying the schedule. |
| Tried to delete an already-deleted task | `409` | Message identifying the task. |
| Created a task with a duplicate `(service, name)` pair | `409` | Message: `Task '<name>' already exists in service '<service>'`. A task's service and name pair must be unique among live tasks. Delete the existing task first, or use a different name. |
| Renamed a task into a name already taken in its service | `409` | Message: `Task '<name>' already exists in service '<service>'`. Soft-deleted tasks do not block the name — only live tasks. |
| Created a destination with a duplicate `destinationId` | `409` | Message identifying the duplicate id. |
| Deleted a destination still referenced by tasks | `409` | Message identifying the destination. |

All error responses use the same shape:

```json
{ "error": "<human-readable message>" }
```

---

## Next steps

- See the full [API reference](api.md) for every field, constraint, and error
  code.
- A ready-made collection of all 18 scenarios (happy path + every error case)
  is available in [`http/tasks.http`](../../http/tasks.http) for use with
  IntelliJ HTTP Client or compatible tools.
