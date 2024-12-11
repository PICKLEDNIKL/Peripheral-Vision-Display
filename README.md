Main Features and Functionalities - 

Google Directions API Integration:
    The DirectionsTask class retrieves and parses directions data from the Google Directions API.
    JSON responses are parsed to extract navigation steps, which are then displayed on a map using Google Maps.

Bluetooth Integration:
    BluetoothActivity handles Bluetooth connections, discovers devices, and facilitates communication with Bluetooth LE services.
    The DirectionForegroundService sends navigation steps to connected Bluetooth devices.

Microcontroller Code:
    The microcontroller code controls LED animations for navigation directions (left, right, straight, turn).
    LEDs are controlled based on messages received, with different animations for motion and non-motion states.

Settings and Preferences:
    SettingsActivity allows users to configure LED colors for different navigation directions.
    SharedPreferences are used to store user preferences for LED settings and brightness.
