# Flutter WebSocket Integration Guide

## Overview

The backend now uses standard WebSocket (not Socket.IO or STOMP) for real-time communication. This is fully compatible with Flutter's `web_socket_channel` package.

## Connection

### Endpoint
```
ws://your-backend-host:8080/ws
```

For production with SSL:
```
wss://your-backend-host:8080/ws
```

### Flutter Code Example

```dart
import 'package:web_socket_channel/web_socket_channel.dart';

// Connect to WebSocket
final channel = WebSocketChannel.connect(
  Uri.parse('ws://your-backend-host:8080/ws'),
);

// Listen for messages
channel.stream.listen(
  (message) {
    final data = jsonDecode(message);
    final event = data['event'];
    final eventData = data['data'];
    
    switch (event) {
      case 'connected':
        print('Connected: ${eventData['sessionId']}');
        break;
      case 'ride_status':
        handleRideStatus(eventData);
        break;
      case 'driver_location':
        handleDriverLocation(eventData);
        break;
      case 'chat_message':
        handleChatMessage(eventData);
        break;
      // ... handle other events
    }
  },
  onError: (error) => print('WebSocket error: $error'),
  onDone: () => print('WebSocket closed'),
);
```

## Message Format

All messages follow this JSON structure:

```json
{
  "event": "event_name",
  "data": { /* event-specific data */ },
  "timestamp": 1234567890
}
```

## Joining Rooms

To receive updates for specific entities (ride, user, driver), join the corresponding room:

```dart
// Join a ride room
channel.sink.add(jsonEncode({
  'event': 'join',
  'type': 'ride',
  'id': '123'  // rideId
}));

// Join a user room
channel.sink.add(jsonEncode({
  'event': 'join',
  'type': 'user',
  'id': 'userId123'
}));

// Join a driver room
channel.sink.add(jsonEncode({
  'event': 'join',
  'type': 'driver',
  'id': '456'  // driverId
}));

// Join fleet monitoring (for admin)
channel.sink.add(jsonEncode({
  'event': 'join',
  'type': 'fleet',
  'id': 'monitoring'
}));
```

After joining, you'll receive a confirmation:

```json
{
  "event": "joined",
  "data": {
    "room": "ride:123",
    "type": "ride"
  },
  "timestamp": 1234567890
}
```

## Available Events

### Client → Server Events

#### 1. Join Room
```json
{
  "event": "join",
  "type": "ride|user|driver|fleet|drivers",
  "id": "roomId"
}
```

#### 2. Leave Room
```json
{
  "event": "leave",
  "type": "ride|user|driver|fleet|drivers",
  "id": "roomId"
}
```

#### 3. Location Update (from driver)
```json
{
  "event": "location",
  "rideId": 123,
  "driverId": 456,
  "lat": 17.5,
  "lng": 78.3,
  "heading": 90  // optional
}
```

#### 4. Chat Message
```json
{
  "event": "chat",
  "rideId": 123,
  "senderId": "user123",
  "senderName": "John Doe",
  "receiverId": "driver456",
  "message": "Hello",
  "messageId": "msg-123"
}
```

#### 5. Ping (keep-alive)
```json
{
  "event": "ping"
}
```

### Server → Client Events

#### 1. Connected
```json
{
  "event": "connected",
  "data": {
    "sessionId": "session-uuid",
    "message": "Connected to WebSocket server"
  },
  "timestamp": 1234567890
}
```

#### 2. Ride Status Update
```json
{
  "event": "ride_status",
  "data": {
    "rideId": 123,
    "status": "ACCEPTED|STARTED|COMPLETED|CANCELLED",
    "ride": { /* RideDto object */ },
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 3. New Ride Request (for drivers)
```json
{
  "event": "new_ride_request",
  "data": {
    "rideId": 123,
    "ride": { /* RideDto object */ },
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 4. Driver Location Update
```json
{
  "event": "driver_location",
  "data": {
    "rideId": 123,
    "driverId": 456,
    "lat": 17.5,
    "lng": 78.3,
    "heading": 90,
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 5. Driver Status Update
```json
{
  "event": "driver_status",
  "data": {
    "driverId": 456,
    "isOnline": true,
    "latitude": 17.5,
    "longitude": 78.3,
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 6. Wallet Update
```json
{
  "event": "wallet_update",
  "data": {
    "userId": "user123",
    "ownerType": "USER|DRIVER",
    "balance": 1000.50,
    "transactionType": "RIDE_PAYMENT",
    "description": "Payment for ride #123",
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 7. Chat Message
```json
{
  "event": "chat_message",
  "data": {
    "rideId": 123,
    "senderId": "user123",
    "senderName": "John Doe",
    "receiverId": "driver456",
    "message": "Hello",
    "messageId": "msg-123",
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 8. Fleet Stats (for admin)
```json
{
  "event": "fleet_stats",
  "data": {
    "totalDrivers": 100,
    "onlineDrivers": 50,
    "activeRides": 25,
    "timestamp": "2024-01-01T12:00:00"
  },
  "timestamp": 1234567890
}
```

#### 9. Pong (response to ping)
```json
{
  "event": "pong",
  "data": {
    "timestamp": 1234567890
  },
  "timestamp": 1234567890
}
```

#### 10. Error
```json
{
  "event": "error",
  "data": {
    "message": "Error description"
  },
  "timestamp": 1234567890
}
```

## Complete Flutter Example

```dart
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';

class WebSocketService {
  WebSocketChannel? _channel;
  
  void connect(String url) {
    _channel = WebSocketChannel.connect(Uri.parse(url));
    
    _channel!.stream.listen(
      (message) {
        final data = jsonDecode(message);
        _handleMessage(data);
      },
      onError: (error) => print('WebSocket error: $error'),
      onDone: () => print('WebSocket closed'),
    );
  }
  
  void _handleMessage(Map<String, dynamic> data) {
    final event = data['event'];
    final eventData = data['data'];
    
    switch (event) {
      case 'connected':
        print('Connected: ${eventData['sessionId']}');
        break;
      case 'ride_status':
        // Handle ride status update
        break;
      case 'driver_location':
        // Handle driver location update
        break;
      // ... handle other events
    }
  }
  
  void joinRide(String rideId) {
    _channel?.sink.add(jsonEncode({
      'event': 'join',
      'type': 'ride',
      'id': rideId,
    }));
  }
  
  void sendLocation(int rideId, int driverId, double lat, double lng) {
    _channel?.sink.add(jsonEncode({
      'event': 'location',
      'rideId': rideId,
      'driverId': driverId,
      'lat': lat,
      'lng': lng,
    }));
  }
  
  void sendChatMessage(int rideId, String senderId, String message) {
    _channel?.sink.add(jsonEncode({
      'event': 'chat',
      'rideId': rideId,
      'senderId': senderId,
      'message': message,
    }));
  }
  
  void disconnect() {
    _channel?.sink.close();
  }
}
```

## Room Types

- `ride:{rideId}` - Updates for a specific ride
- `user:{userId}` - Updates for a specific user
- `driver:{driverId}` - Updates for a specific driver
- `fleet:monitoring` - Fleet monitoring (admin only)
- `drivers:available` - New ride requests (for available drivers)

## Notes

1. **Authentication**: Currently, WebSocket connections don't require authentication. You may want to add JWT token validation in the connection handler for production.

2. **Reconnection**: Implement automatic reconnection logic in your Flutter app to handle network interruptions.

3. **Heartbeat**: The server supports ping/pong for keep-alive. Send a ping every 30 seconds to keep the connection alive.

4. **Error Handling**: Always handle errors and connection closures gracefully.

5. **Room Management**: Remember to leave rooms when no longer needed to reduce server load.

## Migration from Socket.IO

If you were using Socket.IO before:

- **Old**: `socket.emit('join', { type: 'ride', id: 123 })`
- **New**: Send JSON: `{"event": "join", "type": "ride", "id": "123"}`

- **Old**: `socket.on('ride_status', callback)`
- **New**: Listen to all messages and filter by `event` field

The new implementation is simpler and more standard, making it easier to integrate with Flutter.

