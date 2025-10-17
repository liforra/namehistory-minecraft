# API Documentation

This mod connects to a REST API to fetch player name history. The default endpoint is `https://liforra.de/api/namehistory`.

## Endpoints

### Get History by Username
```
GET /api/namehistory?username={username}
```

**Parameters:**
- `username` (string): The player's current or previous username

**Response:**
```json
{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
  "query": "Notch",
  "history": [
    {
      "id": 1,
      "name": "Notch",
      "changedAt": null,
      "censored": false
    }
  ]
}
```

### Get History by UUID
```
GET /api/namehistory/uuid/{uuid}
```

**Parameters:**
- `uuid` (string): The player's UUID (with or without dashes)

**Response:**
Same as username endpoint

### Force Update
```
POST /api/namehistory/update
Content-Type: application/json
```

**Body:**
```json
{
  "username": "Notch"
}
```
or
```json
{
  "uuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5"
}
```
or for batch updates:
```json
{
  "usernames": ["Notch", "jeb_"]
}
```
or
```json
{
  "uuids": ["069a79f4-44e9-4726-a5be-fca90e38aaf5"]
}
```

**Response:**
Same as GET endpoints

### Delete Cached Entry
```
DELETE /api/namehistory?username={username}
```
or
```
DELETE /api/namehistory?uuid={uuid}
```

**Response:**
```json
{
  "success": true,
  "message": "Cache entry deleted"
}
```

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `uuid` | string | Player's UUID |
| `query` | string | The search query used |
| `history` | array | Array of name history entries |
| `history[].id` | integer | Entry ID (1 = oldest) |
| `history[].name` | string | Username at this point |
| `history[].changedAt` | string\|null | ISO 8601 timestamp or null for original name |
| `history[].censored` | boolean | Whether this name was censored by Mojang |

## Error Responses

### 404 Not Found
```json
{
  "error": "Player not found"
}
```

### 429 Too Many Requests
```json
{
  "error": "Rate limit exceeded"
}
```

### 502 Bad Gateway
```json
{
  "error": "API temporarily unavailable"
}
```

## Authentication

If your API requires authentication, configure it in the mod settings:

1. Set `apiKey` to your API key or token
2. Set `apiKeyHeader` to the header name (e.g., "X-API-Key")

For Bearer tokens:
- Set `apiKeyHeader` to "Authorization"
- Set `apiKey` to "Bearer YOUR_TOKEN_HERE"

## Rate Limiting

The mod implements client-side caching to reduce API calls. The default cache TTL is 10 minutes, configurable in the mod settings.

## Custom API Implementation

To use your own API server:

1. Implement the endpoints listed above
2. Update `baseUrl` in the mod configuration
3. Set authentication if required

The API responses must match the format described in this document.
