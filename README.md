# Peripheral Vision Display: Creating an Unobtrusive LED Notification and Navigation System for Glasses

This project is an example of implementing peripheral vision to aid in user navigation and notification viewing.

The project was designed around the use of adafruit LED glasses. (found here: https://www.adafruit.com/product/5255)

Circuitpython was used on the nRF5284O microcontroller and corresponding files are found in the "microcontroller" folder.

The project proposal and report are both available in their .pdf format showing the process of making the product and other relevant information to better understand what has been made and the results of how effective it was. 

The google api key has been removed but would be needed for any of the google api things to work such as the navigation/map feature.

## Main Features and Functionalities - 

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

~ Year 3 Final Individual Project
