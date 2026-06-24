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

```json
{
  "items": [ ... ],
  "limit": 20,
  "offset": 0
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
| `messageType` | string | Application-level event type (e.g. `booking.expire.v1`). |
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
| `messageType` | string | yes | — | Non-blank. Application-defined event type string. |
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
  "messageType": "booking.expire.v1",
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
  "messageType": "booking.expire.v1",
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
      "messageType": "booking.expire.v1",
      "payload": { "bookingId": "abc-123", "reason": "hold_expired" },
      "timeoutMs": 5000,
      "supportsRetry": true,
      "createdAt": "2026-06-24T10:15:30.123456Z",
      "updatedAt": "2026-06-24T10:15:30.123456Z"
    }
  ],
  "limit": 20,
  "offset": 0
}
```

**Example — explicit pagination**

```http
GET /api/v1/tasks?limit=5&offset=0
```

The response echoes back the effective `limit` and `offset`:

```json
{
  "items": [ ... ],
  "limit": 5,
  "offset": 0
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
  "messageType": "booking.expire.v1",
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
| `messageType` | string | yes | — | Non-blank. |
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
  "messageType": "booking.expire.v2",
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
  "messageType": "booking.expire.v2",
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

## Error reference

| HTTP Status | Condition |
|---|---|
| `400 Bad Request` | Missing required field; `timeoutMs <= 0`; `destinationId` references a non-existent destination; path `id` is not a valid UUID string. |
| `404 Not Found` | Task with the given `id` does not exist, or has been soft-deleted. |
| `409 Conflict` | Attempting to delete a task that is already deleted. |
| `500 Internal Server Error` | An unexpected error occurred. The response body contains `{ "error": "Internal server error" }`. No internal detail is exposed. |
