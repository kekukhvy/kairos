# Kairos REST API Reference

Base URL: `http://<host>:8080`  
All paths are prefixed with `/api/v1`.  
All request and response bodies are JSON (`Content-Type: application/json`).  
Timestamps are ISO-8601 strings in UTC (e.g. `"2026-06-24T10:15:30.123456Z"`).  
Unknown fields in request bodies are silently ignored (forward-compatible).

---

## Common structures

### ErrorResponse

Returned for every `4xx` and `5xx` response. Always a single field.

```json
{ "error": "<human-readable message>" }
```

### PageResponse

Wraps any paginated list. The `limit` and `offset` echoed back are the
**effective** values after normalization (see [Pagination](#pagination)).
`hasNext` is `true` when there are more items beyond the current page.

```json
{
  "items": [ ... ],
  "limit": 20,
  "offset": 0,
  "hasNext": false
}
```

### Pagination

Query parameters `limit` and `offset` are accepted on list endpoints. Both are
optional. The server normalizes them:

| Condition | Effective value |
|---|---|
| `limit` absent or `<= 0` | `20` (default) |
| `limit > 100` | `100` (maximum) |
| `offset` absent or `< 0` | `0` |

Valid range for an explicit `limit`: `1–100`.

---

## TaskResponse

All task endpoints that return a body use this structure.

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | Unique identifier assigned by Kairos. |
| `service` | string | The client service that owns this task. Immutable. |
| `name` | string | Human-readable name for the task. |
| `description` | string | Optional description. |
| `active` | boolean | Whether the task is active. |
| `destinationId` | string | ID of the destination to deliver to. |
| `eventName` | string | Machine-readable, versioned event type the consumer uses to route handling (e.g. `booking.expire.v1`). Distinct from the human-readable task name. |
| `payload` | any JSON or `null` | Arbitrary JSON value delivered with the message. |
| `timeoutMs` | integer | Maximum delivery time in milliseconds. |
| `supportsRetry` | boolean | Whether failed deliveries should be retried. |
| `createdAt` | ISO-8601 string | When the task was created. |
| `updatedAt` | ISO-8601 string | When the task was last updated. |

Deleted tasks are never returned — callers receive `404` instead.

---

## Endpoints

### POST /api/v1/tasks — Create a task

Creates a new task. Returns the created task with its server-assigned `id` and timestamps.

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `service` | string | yes | — | Non-blank. Immutable after creation. |
| `name` | string | yes | — | Non-blank. |
| `description` | string | no | `null` | — |
| `active` | boolean | no | `true` | Omit or send `null` to accept the default. |
| `destinationId` | string | yes | — | Must reference an existing destination row. |
| `eventName` | string | yes | — | Non-blank. Machine-readable, versioned event type the consumer uses to route handling (e.g. `booking.expire.v1`). Distinct from the human-readable task name. |
| `payload` | any JSON | no | `null` | Any valid JSON value (object, array, string, number, etc.). Stored as JSONB. |
| `timeoutMs` | integer | yes | — | Must be `> 0`. |
| `supportsRetry` | boolean | no | `false` | Omit or send `null` to accept the default. |

**Responses**

| Status | Body | When |
|---|---|---|
| `201 Created` | `TaskResponse` | Task created successfully. |
| `400 Bad Request` | `ErrorResponse` | Missing required field, `timeoutMs <= 0`, or `destinationId` does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
POST /api/v1/tasks
Content-Type: application/json

{
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Cancels a reservation after the hold window expires",
  "active": true,
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

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Cancels a reservation after the hold window expires",
  "active": true,
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v1",
  "payload": {
    "bookingId": "abc-123",
    "reason": "hold_expired"
  },
  "timeoutMs": 5000,
  "supportsRetry": true,
  "createdAt": "2026-06-24T10:15:30.123456Z",
  "updatedAt": "2026-06-24T10:15:30.123456Z"
}
```

---

### GET /api/v1/tasks — List tasks

Returns a paginated list of all non-deleted tasks. Deleted tasks are excluded.

**Query parameters**

| Parameter | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `limit` | integer | no | `20` | Clamped to `1–100`. Values `<= 0` use the default. Values `> 100` are capped at `100`. |
| `offset` | integer | no | `0` | Values `< 0` are treated as `0`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `PageResponse<TaskResponse>` | Always (empty `items` array if no tasks exist). |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example — default pagination**

```http
GET /api/v1/tasks
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "items": [
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
  ],
  "limit": 20,
  "offset": 0,
  "hasNext": false
}
```

**Example — explicit pagination**

```http
GET /api/v1/tasks?limit=5&offset=0
```

The response echoes back the effective `limit` and `offset`, and signals
whether a further page exists:

```json
{
  "items": [ ... ],
  "limit": 5,
  "offset": 0,
  "hasNext": true
}
```

---

### GET /api/v1/tasks/{id} — Get a task

Returns a single task by its UUID.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The task's `id` as returned at creation. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `TaskResponse` | Task found. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Task does not exist or has been deleted. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
GET /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

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

**Error example — malformed UUID**

```http
GET /api/v1/tasks/not-a-uuid
```

```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{ "error": "Invalid UUID: not-a-uuid" }
```

**Error example — not found**

```http
GET /api/v1/tasks/00000000-0000-0000-0000-000000000000
```

```json
HTTP/1.1 404 Not Found
Content-Type: application/json

{ "error": "Task not found: 00000000-0000-0000-0000-000000000000" }
```

---

### PUT /api/v1/tasks/{id} — Update a task

Full replacement update. All editable fields must be supplied; the server
replaces the task's editable field set with the values in the request body.

`service` is **not** an editable field — a task's owning service is immutable
and must not be included in the request body.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The task's `id`. |

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `name` | string | yes | — | Non-blank. |
| `description` | string | no | `null` | — |
| `active` | boolean | no | `true` | Omit or send `null` to accept the default. |
| `destinationId` | string | yes | — | Must reference an existing destination row. |
| `eventName` | string | yes | — | Non-blank. Machine-readable, versioned event type the consumer uses to route handling (e.g. `booking.expire.v1`). Distinct from the human-readable task name. |
| `payload` | any JSON | no | `null` | Any valid JSON value. Send `null` explicitly to clear. |
| `timeoutMs` | integer | yes | — | Must be `> 0`. |
| `supportsRetry` | boolean | no | `false` | Omit or send `null` to accept the default. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `TaskResponse` | Task updated successfully. |
| `400 Bad Request` | `ErrorResponse` | Validation error or malformed `id`. |
| `404 Not Found` | `ErrorResponse` | Task does not exist or has been deleted. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
PUT /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
Content-Type: application/json

{
  "name": "expire-booking",
  "description": "Updated description after retry policy change",
  "active": true,
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v2",
  "payload": {
    "bookingId": "abc-123",
    "reason": "hold_expired",
    "version": 2
  },
  "timeoutMs": 10000,
  "supportsRetry": true
}
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Updated description after retry policy change",
  "active": true,
  "destinationId": "booking-kafka",
  "eventName": "booking.expire.v2",
  "payload": {
    "bookingId": "abc-123",
    "reason": "hold_expired",
    "version": 2
  },
  "timeoutMs": 10000,
  "supportsRetry": true,
  "createdAt": "2026-06-24T10:15:30.123456Z",
  "updatedAt": "2026-06-24T10:22:05.654321Z"
}
```

---

### DELETE /api/v1/tasks/{id} — Delete a task

Soft-deletes a task. The task is marked as deleted and will no longer appear in
any response. The operation is not reversible through the API.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The task's `id`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `204 No Content` | none | Task successfully deleted. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Task does not exist (never created or already deleted and then looked up via this path after a restart — see 409 below for the in-session case). |
| `409 Conflict` | `ErrorResponse` | Task has already been deleted. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
DELETE /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

```
HTTP/1.1 204 No Content
```

**Error example — already deleted**

```http
DELETE /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

```json
HTTP/1.1 409 Conflict
Content-Type: application/json

{ "error": "Task already deleted: f47ac10b-58cc-4372-a567-0e02b2c3d479" }
```

---

## DestinationResponse

All destination endpoints that return a body use this structure.

| Field | Type | Description |
|---|---|---|
| `destinationId` | string | The client-supplied identifier for this destination. Unique across all destinations. |
| `destinationType` | string | The delivery mechanism (e.g. `KAFKA`). Must be a recognized type. |
| `config` | JSON object | Destination-specific configuration (e.g. `{"topic": "bookings"}`). Must contain the required keys for its type (see [Destination config schema](#destination-config-schema)); values are not inspected, and extra keys are stored and returned as-is. |
| `createdAt` | ISO-8601 string | When the destination was created. |

### Destination config schema

The `config` field is a JSON object whose shape is validated according to the
destination's type. Each type requires specific keys to be present; extra keys
beyond the required and optional set are accepted and stored untouched.

**Only key presence is enforced** — values are never inspected or validated. An
empty string, `null`, or any other JSON value is accepted as long as the key
exists. Delivery adapters may later inspect and validate the values for their
specific type.

| Type | Required keys | Optional keys | Example |
|---|---|---|---|
| `KAFKA` | `topic` | `key`, `headers` | `{"topic": "orders"}` |
| `SQS` | `queueUrl` | `messageGroupId` | `{"queueUrl": "https://sqs.us-east-1.amazonaws.com/123456789/myqueue"}` |
| `WEBHOOK` | `url` | `method`, `headers` | `{"url": "https://example.com/hook"}` |
| `RABBITMQ` | `exchange`, `routingKey` | `headers` | `{"exchange": "orders", "routingKey": "order.created"}` |

**Example: valid config with optional keys**

```json
{
  "topic": "bookings",
  "key": "booking-id",
  "headers": {"service-name": "booking-service"},
  "myCustomKey": 42
}
```

The above is valid for type `KAFKA`: it has the required `topic` key, includes
optional keys `key` and `headers`, and adds a custom `myCustomKey` which is
preserved but not validated by Kairos.

---

### POST /api/v1/destinations — Create a destination

Creates a new destination. The `destinationId` is chosen by the caller and must
be unique. Returns the created destination.

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `destinationId` | string | yes | — | Caller-chosen unique identifier. Must not already exist. |
| `destinationType` | string | yes | — | Must be a recognized delivery type: `KAFKA`, `SQS`, `WEBHOOK`, or `RABBITMQ`. |
| `config` | JSON object | yes | — | Must be a valid JSON object (not `null`, not an array). Must contain all required keys for the given `destinationType` (see [Destination config schema](#destination-config-schema)). Extra keys are allowed. |

**Responses**

| Status | Body | When |
|---|---|---|
| `201 Created` | `DestinationResponse` | Destination created successfully. |
| `400 Bad Request` | `ErrorResponse` | Missing required field; `config` is not valid JSON or is not a JSON object; `config` is null/blank; `config` omits a required key for the given `destinationType`; or unrecognized `destinationType`. |
| `409 Conflict` | `ErrorResponse` | A destination with the given `destinationId` already exists. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example — successful creation**

```http
POST /api/v1/destinations
Content-Type: application/json

{
  "destinationId": "booking-kafka",
  "destinationType": "KAFKA",
  "config": {
    "topic": "bookings"
  }
}
```

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "destinationId": "booking-kafka",
  "destinationType": "KAFKA",
  "config": {
    "topic": "bookings"
  },
  "createdAt": "2026-06-24T10:00:00.000000Z"
}
```

**Error example — duplicate id**

```json
HTTP/1.1 409 Conflict
Content-Type: application/json

{ "error": "Destination already exists: booking-kafka" }
```

**Error example — unknown type**

```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{ "error": "Invalid destination type: UNKNOWN" }
```

**Error example — missing required config key**

```http
POST /api/v1/destinations
Content-Type: application/json

{
  "destinationId": "booking-rabbitmq",
  "destinationType": "RABBITMQ",
  "config": {}
}
```

```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{ "error": "config is missing required key(s): exchange, routingKey" }
```

---

### GET /api/v1/destinations — List destinations

Returns a paginated list of all destinations.

**Query parameters**

| Parameter | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `limit` | integer | no | `20` | Clamped to `1–100`. Values `<= 0` use the default. Values `> 100` are capped at `100`. |
| `offset` | integer | no | `0` | Values `< 0` are treated as `0`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `PageResponse<DestinationResponse>` | Always (empty `items` array if no destinations exist). |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
GET /api/v1/destinations
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "items": [
    {
      "destinationId": "booking-kafka",
      "destinationType": "KAFKA",
      "config": { "topic": "bookings" },
      "createdAt": "2026-06-24T10:00:00.000000Z"
    }
  ],
  "limit": 20,
  "offset": 0,
  "hasNext": false
}
```

**Example — explicit pagination**

```http
GET /api/v1/destinations?limit=5&offset=0
```

```json
{
  "items": [ ... ],
  "limit": 5,
  "offset": 0,
  "hasNext": true
}
```

---

### GET /api/v1/destinations/{id} — Get a destination

Returns a single destination by its `destinationId`.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | string | The destination's `destinationId` as supplied at creation. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `DestinationResponse` | Destination found. |
| `404 Not Found` | `ErrorResponse` | Destination does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
GET /api/v1/destinations/booking-kafka
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "destinationId": "booking-kafka",
  "destinationType": "KAFKA",
  "config": { "topic": "bookings" },
  "createdAt": "2026-06-24T10:00:00.000000Z"
}
```

**Error example — not found**

```json
HTTP/1.1 404 Not Found
Content-Type: application/json

{ "error": "Destination not found: booking-kafka" }
```

---

### PUT /api/v1/destinations/{id} — Update a destination

Updates the `config` of an existing destination. `destinationId` and
`destinationType` are immutable and cannot be changed. The new config is
validated against the destination's stored type's schema (see [Destination config
schema](#destination-config-schema)).

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | string | The destination's `destinationId`. |

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `config` | JSON object | yes | — | Replaces the current config. Must be a valid JSON object (not `null`, not an array). Must contain all required keys for the destination's type (determined at creation). Extra keys are allowed. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `DestinationResponse` | Destination updated successfully. |
| `400 Bad Request` | `ErrorResponse` | `config` is not valid JSON or is not a JSON object; `config` is null/blank; or `config` omits a required key for the destination's stored type. |
| `404 Not Found` | `ErrorResponse` | Destination does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example — successful update**

```http
PUT /api/v1/destinations/booking-kafka
Content-Type: application/json

{
  "config": {
    "topic": "bookings-v2",
    "partitions": 3
  }
}
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "destinationId": "booking-kafka",
  "destinationType": "KAFKA",
  "config": {
    "topic": "bookings-v2",
    "partitions": 3
  },
  "createdAt": "2026-06-24T10:00:00.000000Z"
}
```

**Error example — missing required config key**

```http
PUT /api/v1/destinations/booking-kafka
Content-Type: application/json

{
  "config": {}
}
```

```json
HTTP/1.1 400 Bad Request
Content-Type: application/json

{ "error": "config is missing required key(s): topic" }
```

---

### DELETE /api/v1/destinations/{id} — Delete a destination

Deletes a destination permanently. The destination must not be referenced by any
existing task. If any task still references this destination, the request is
rejected with `409 Conflict`.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | string | The destination's `destinationId`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `204 No Content` | none | Destination deleted successfully. |
| `404 Not Found` | `ErrorResponse` | Destination does not exist. |
| `409 Conflict` | `ErrorResponse` | One or more tasks still reference this destination. Delete or reassign those tasks first. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
DELETE /api/v1/destinations/booking-kafka
```

```
HTTP/1.1 204 No Content
```

**Error example — destination still in use**

```json
HTTP/1.1 409 Conflict
Content-Type: application/json

{ "error": "Destination is in use: booking-kafka" }
```

---

## ScheduleResponse

All schedule endpoints that return a body use this structure. Fields that do not
apply to the schedule's `type` are `null`.

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | Unique identifier assigned by Kairos. |
| `taskId` | UUID string | The task this schedule belongs to. |
| `type` | string | `ONCE`, `CRON`, or `FIXED`. Immutable after creation. |
| `label` | string or `null` | Optional human-readable label (max 128 characters). |
| `runAt` | ISO-8601 string or `null` | `ONCE` only: the single fire time (UTC). |
| `cronExpression` | string or `null` | `CRON` only: the cron expression string (max 128 characters). Syntax is not validated by the server. |
| `intervalSeconds` | integer or `null` | `FIXED` only: the repeat interval in seconds (`1–86400`). |
| `timezone` | string | IANA timezone name used to evaluate `cronExpression`. Always `UTC` for `ONCE` and `FIXED` schedules. Defaults to `UTC`. |
| `active` | boolean | Whether this schedule is currently active. `true` by default at creation. |
| `createdAt` | ISO-8601 string | When the schedule was created. |
| `updatedAt` | ISO-8601 string | When the schedule was last updated. |

---

### POST /api/v1/tasks/{taskId}/schedules — Create a schedule

Creates a new schedule for an existing task. Returns the created schedule.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | UUID string | The `id` of the task to schedule. Must exist and not be deleted. |

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `type` | string | yes | — | `ONCE`, `CRON`, or `FIXED`. |
| `label` | string | no | `null` | Optional. Max 128 characters. |
| `runAt` | ISO-8601 string | `ONCE` only | — | Must be a future timestamp. Ignored for `CRON` and `FIXED`. |
| `cronExpression` | string | `CRON` only | — | Non-blank, max 128 characters. Not syntax-validated by the server. Ignored for `ONCE` and `FIXED`. |
| `intervalSeconds` | integer | `FIXED` only | — | `1–86400` (one second to one day). Ignored for `ONCE` and `CRON`. |
| `timezone` | string | `CRON` only | `UTC` | Valid IANA timezone name (e.g. `Europe/Berlin`). Ignored for `ONCE` and `FIXED`. |

Only send the "when" field that matches `type`; the others should be omitted (or `null`).

**Responses**

| Status | Body | When |
|---|---|---|
| `201 Created` | `ScheduleResponse` | Schedule created successfully. |
| `400 Bad Request` | `ErrorResponse` | Missing or invalid `type`; missing required "when" field for the given type; `runAt` is in the past; `intervalSeconds` out of range; `timezone` is not a valid IANA zone id; malformed UUID path param. |
| `404 Not Found` | `ErrorResponse` | Task with the given `taskId` does not exist or has been deleted. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example — ONCE schedule**

```http
POST /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479/schedules
Content-Type: application/json

{
  "type": "ONCE",
  "label": "expire-hold-abc123",
  "runAt": "2026-07-10T14:00:00Z"
}
```

```json
HTTP/1.1 201 Created
Content-Type: application/json

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

**Example — CRON schedule**

```http
POST /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479/schedules
Content-Type: application/json

{
  "type": "CRON",
  "label": "nightly-cleanup",
  "cronExpression": "0 2 * * *",
  "timezone": "Europe/Berlin"
}
```

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "a1b2c3d4-0000-0000-0000-222222222222",
  "taskId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "type": "CRON",
  "label": "nightly-cleanup",
  "runAt": null,
  "cronExpression": "0 2 * * *",
  "intervalSeconds": null,
  "timezone": "Europe/Berlin",
  "active": true,
  "createdAt": "2026-07-09T09:00:00Z",
  "updatedAt": "2026-07-09T09:00:00Z"
}
```

**Example — FIXED schedule**

```http
POST /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479/schedules
Content-Type: application/json

{
  "type": "FIXED",
  "label": "heartbeat",
  "intervalSeconds": 300
}
```

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "a1b2c3d4-0000-0000-0000-333333333333",
  "taskId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "type": "FIXED",
  "label": "heartbeat",
  "runAt": null,
  "cronExpression": null,
  "intervalSeconds": 300,
  "timezone": "UTC",
  "active": true,
  "createdAt": "2026-07-09T09:00:00Z",
  "updatedAt": "2026-07-09T09:00:00Z"
}
```

---

### GET /api/v1/tasks/{taskId}/schedules — List schedules for a task

Returns a paginated list of all schedules belonging to a task.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `taskId` | UUID string | The `id` of the task. |

**Query parameters**

| Parameter | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `limit` | integer | no | `20` | Clamped to `1–100`. Values `<= 0` use the default. Values `> 100` are capped at `100`. |
| `offset` | integer | no | `0` | Values `< 0` are treated as `0`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `PageResponse<ScheduleResponse>` | Always (empty `items` array if the task has no schedules). |
| `400 Bad Request` | `ErrorResponse` | `taskId` is not a valid UUID. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
GET /api/v1/tasks/f47ac10b-58cc-4372-a567-0e02b2c3d479/schedules
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "items": [
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
  ],
  "limit": 20,
  "offset": 0,
  "hasNext": false
}
```

---

### GET /api/v1/schedules/{id} — Get a schedule

Returns a single schedule by its UUID.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The schedule's `id` as returned at creation. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `ScheduleResponse` | Schedule found. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Schedule does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
GET /api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

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

---

### PUT /api/v1/schedules/{id} — Update a schedule

Updates the scheduling parameters of an existing schedule. `type` is immutable —
to change schedule type, delete the schedule and create a new one. Only the
"when" field matching the schedule's current `type` is applied; sending fields
for other types has no effect.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The schedule's `id`. |

**Request body**

| Field | Type | Required | Default | Constraints |
|---|---|---|---|---|
| `label` | string | no | `null` | Optional. Max 128 characters. |
| `runAt` | ISO-8601 string | `ONCE` only | — | Must be a future timestamp. Ignored for other types. |
| `cronExpression` | string | `CRON` only | — | Non-blank, max 128 characters. Ignored for other types. |
| `intervalSeconds` | integer | `FIXED` only | — | `1–86400`. Ignored for other types. |
| `timezone` | string | no | `UTC` | Valid IANA timezone name. Applied only for `CRON` schedules; ignored for `ONCE` and `FIXED`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `ScheduleResponse` | Schedule updated successfully. |
| `400 Bad Request` | `ErrorResponse` | Missing required "when" field for the current type; `runAt` is in the past; `intervalSeconds` out of range; invalid timezone; malformed UUID path param. |
| `404 Not Found` | `ErrorResponse` | Schedule does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example — update a ONCE schedule**

```http
PUT /api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111
Content-Type: application/json

{
  "label": "expire-hold-abc123-rescheduled",
  "runAt": "2026-07-11T10:00:00Z"
}
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "a1b2c3d4-0000-0000-0000-111111111111",
  "taskId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "type": "ONCE",
  "label": "expire-hold-abc123-rescheduled",
  "runAt": "2026-07-11T10:00:00Z",
  "cronExpression": null,
  "intervalSeconds": null,
  "timezone": "UTC",
  "active": true,
  "createdAt": "2026-07-09T09:00:00Z",
  "updatedAt": "2026-07-09T09:30:00Z"
}
```

---

### DELETE /api/v1/schedules/{id} — Delete a schedule

Permanently deletes a schedule. The schedule will no longer fire and cannot be
recovered.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The schedule's `id`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `204 No Content` | none | Schedule deleted successfully. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Schedule does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
DELETE /api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111
```

```
HTTP/1.1 204 No Content
```

---

### PATCH /api/v1/schedules/{id}/pause — Pause a schedule

Sets `active` to `false` on the schedule. The schedule stops firing until
resumed. Has no effect on other schedules belonging to the same task.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The schedule's `id`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `ScheduleResponse` | Schedule paused. `active` is `false` in the response. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Schedule does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
PATCH /api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111/pause
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "a1b2c3d4-0000-0000-0000-111111111111",
  "taskId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "type": "ONCE",
  "label": "expire-hold-abc123",
  "runAt": "2026-07-10T14:00:00Z",
  "cronExpression": null,
  "intervalSeconds": null,
  "timezone": "UTC",
  "active": false,
  "createdAt": "2026-07-09T09:00:00Z",
  "updatedAt": "2026-07-09T10:00:00Z"
}
```

---

### PATCH /api/v1/schedules/{id}/resume — Resume a schedule

Sets `active` to `true` on a previously paused schedule. The schedule resumes
firing according to its timing rule.

**Path parameters**

| Parameter | Type | Description |
|---|---|---|
| `id` | UUID string | The schedule's `id`. |

**Responses**

| Status | Body | When |
|---|---|---|
| `200 OK` | `ScheduleResponse` | Schedule resumed. `active` is `true` in the response. |
| `400 Bad Request` | `ErrorResponse` | `id` is not a valid UUID. |
| `404 Not Found` | `ErrorResponse` | Schedule does not exist. |
| `500 Internal Server Error` | `ErrorResponse` | Unexpected server error. |

**Example**

```http
PATCH /api/v1/schedules/a1b2c3d4-0000-0000-0000-111111111111/resume
```

```json
HTTP/1.1 200 OK
Content-Type: application/json

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
  "updatedAt": "2026-07-09T10:05:00Z"
}
```

---

## Error reference

| HTTP Status | Condition |
|---|---|
| `400 Bad Request` | Missing required field; `timeoutMs <= 0`; path `id` is not a valid UUID (tasks, schedules); unrecognized `destinationType` or `type`; `runAt` is not in the future; `intervalSeconds` out of range (`1–86400`); `timezone` is not a valid IANA zone id; `cronExpression` is blank; malformed JSON body. |
| `404 Not Found` | Task or destination with the given `id` does not exist, or the task has been soft-deleted. Schedule with the given `id` does not exist. |
| `409 Conflict` | Attempting to delete a task that is already deleted; creating a destination whose `destinationId` already exists; deleting a destination that is still referenced by one or more tasks. |
| `500 Internal Server Error` | An unexpected error occurred. The response body contains `{ "error": "Internal server error" }`. No internal detail is exposed. |
