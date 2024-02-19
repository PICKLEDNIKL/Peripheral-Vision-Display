import board
import adafruit_is31fl3741
# from rainbowio import colorwheel
from adafruit_is31fl3741.adafruit_ledglasses import LED_Glasses
from adafruit_is31fl3741.is31fl3741_pixelbuf import IS31FL3741_PixelBuf
from adafruit_is31fl3741.led_glasses_map import *
import digitalio
import time
from adafruit_ble import BLERadio
from adafruit_ble.advertising.standard import ProvideServicesAdvertisement
from adafruit_ble.services.nordic import UARTService
from adafruit_ble import BLERadio
from adafruit_ble.attributes import Attribute
# from adafruit_ble.attributes import Characteristic
from adafruit_ble.characteristics import Characteristic
from adafruit_ble.services import Service
from adafruit_ble.uuid import StandardUUID
import busio
from rainbowio import colorwheel
import is31fl3741
import neopixel
# import base64
import binascii


led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT

ble = BLERadio()
uart = UARTService()

advertisement = ProvideServicesAdvertisement(uart)
i2c = board.I2C()  # uses board.SCL and board.SDA
glasses = LED_Glasses(i2c, allocate=adafruit_is31fl3741.MUST_BUFFER)
glasses.show() #clear residual data
glasses.global_current = 20

left_ring = glasses.left_ring

right_ring = glasses.right_ring

pixel_pin = board.NEOPIXEL
pixels = neopixel.NeoPixel(pixel_pin, 1, brightness=0.3, auto_write=False)

RED = 0xFF0000      # red in hex rgb
PURPLE = 0x800080   # purple in hex rgb
BLUE = 0x0000FF     # blue in hex rgb
GREEN = 0x00FF00    # green in hex rgb
ORANGE = 0xFFA500   # orange in hex rgb
YELLOW = 0xFFFF00   # yellow in hex rgb

notifcolor = YELLOW
leftcolor = BLUE
rightcolor = BLUE
strcolor = GREEN
turncolor = RED
motionboolean = True

def apply_settings(decoded_bytes):

    global notifcolor, leftcolor, rightcolor, strcolor, turncolor, motionboolean

    # Define the mappings from byte values to settings and colors
    settings_map = {1: "NOTIF", 2: "LEFT", 3: "RIGHT", 4: "STR", 5: "TURN", 6: "LED"}
    colors_map = {1: RED, 2: PURPLE, 3: BLUE, 4: GREEN, 5: YELLOW, 6: ORANGE, 7: True, 8: False}

    # Iterate over the bytes in pairs
    for i in range(0, len(decoded_bytes), 2):
        # Get the setting and color from the byte values
        setting = settings_map.get(decoded_bytes[i], "UNKNOWN")
        color = colors_map.get(decoded_bytes[i + 1], "UNKNOWN")

        # Apply the setting
        print(f"Applying setting {setting} with color {color}")

        # Here, you can call the appropriate method to apply the setting
        # For example:
        if setting == "NOTIF":
            notifcolor = color
        elif setting == "LEFT":
            leftcolor = color
        elif setting == "RIGHT":
            rightcolor = color
        elif setting == "STR":
            strcolor = color
        elif setting == "TURN":
            turncolor = color
        elif setting == "LED":
            motionboolean = color

while True:
    ble.start_advertising(advertisement)
    while not ble.connected:
        #check if trying to connect
        # led.value = True
        # time.sleep(0.5)
        # led.value = False
        # time.sleep(0.5)
        pixels.fill((255, 0, 0))
        pixels.show()
        time.sleep(0.4)
        pixels.fill((0, 0, 255))
        pixels.show()
        time.sleep(0.4)
        pass

    # Now we're connected

    glasses.fill(2)
    glasses.show()
    time.sleep(0.5)
    glasses.fill(0)
    glasses.show()
    led.value = True
    time.sleep(0.5)
    led.value = False

#todo: need to either check for new line so add \n to the end of the message and then use readline instead. im pretty sure that the main reason for the issue is because the notifications get duplicated somewhere on the app.

    while ble.connected:
        if uart.in_waiting:
            # TESTING FOR NOTIFICATION ADDITION
            data = uart.read(32)
            if data is not None:
                message = data.decode("utf-8")
                print("message received")
                print(message)
                # NOTIF
                if message.startswith('LEFT'):
                    # for count in range(0, 2):
                    # light up all of left ring
                    # for i in range(0, 24):
                    left_ring[0] = notifcolor
                    left_ring[6] = notifcolor
                    left_ring[12] = notifcolor
                    left_ring[18] = notifcolor

                    # light up all of right ring
                    # for i in range(0, 24):
                    right_ring[0] = notifcolor
                    right_ring[6] = notifcolor
                    right_ring[12] = notifcolor
                    right_ring[18] = notifcolor
                    glasses.show()
                    time.sleep(0.5) # How long the light stays on

                    # reset leds
                    for i in range(0, 24):
                        left_ring[i] = 0
                        right_ring[i] = 0
                    glasses.show()
                    time.sleep(0.25)
                # LEFT
                elif message.startswith('NOTIF'):
                    for count in range(0, 2):
                        # light up the left side of the left ring
                        if motionboolean:
                            for i in range(14, 23):
                                left_ring[i] = leftcolor
                                print(leftcolor)
                                glasses.show()
                                time.sleep(0.05)

                            for i in range(14, 23):
                                left_ring[i] = 0
                                glasses.show()
                                time.sleep(0.05)
                        elif not motionboolean:
                            for i in range(14, 23):
                                left_ring[i] = leftcolor # blue in hex rgb
                            glasses.show()
                            time.sleep(0.5)

                            for i in range(14, 23):
                                left_ring[i] = 0
                            glasses.show()
                            time.sleep(0.5)
                            
                # RIGHT
                elif message.startswith('RIGHT'):
                    for count in range(0, 2):
                        # light up the right side of the right ring
                        for i in range(2, 10):
                            right_ring[i] = rightcolor # blue in hex rgb
                            glasses.show()
                            time.sleep(0.05)

                        for i in range(2, 10):
                            right_ring[i] = 0
                            glasses.show()
                            time.sleep(0.05)

                #STR
                elif message.startswith('STR'):
                    for count in range(0, 2):
                        # light up top of left ring
                        for i in range(0, 3):
                            left_ring[i] = strcolor
                        for i in range(21, 24):
                            left_ring[i] = strcolor

                        # light up top of right ring
                        for i in range(0, 4):
                            right_ring[i] = strcolor
                        for i in range(22, 24):
                            right_ring[i] = strcolor
                        glasses.show()

                        # reset leds
                        time.sleep(0.5)
                        for i in range(0, 24):
                            left_ring[i] = 0
                            right_ring[i] = 0
                        glasses.show()
                        time.sleep(0.25)
                # TURN
                elif message.startswith('TURN'):
                    #led from the bottom - MAYBE MAKE IT TO DO AN ANIMATION LATER ON
                    for count in range(0, 2):
                        # light up bottom of left ring
                        for i in range(9, 16):
                            left_ring[i] = turncolor

                        # light up bottom of right ring
                        for i in range(9, 16):
                            right_ring[i] = turncolor
                        glasses.show()

                        # reset leds
                        time.sleep(0.5)
                        for i in range(0, 24):
                            left_ring[i] = 0
                            right_ring[i] = 0
                        glasses.show()
                        time.sleep(0.25)
                else:
                    
                    # Check size of message. If the message has a length less than 16, this message is not for setting configuration.
                    # If the size of the message is more than 16, there was likely an issue with messages being sent too quickly. 
                    if len(message) < 16:
                        print("Message too short")
                        break
                    elif len(message) > 16:
                        message = message[:16]

                    decoded_bytes = binascii.a2b_base64(message)
                    print(decoded_bytes)

                    # Check the structure of the message
                    settingscheck = decoded_bytes[0] != 0x01 or decoded_bytes[2] != 0x02 or decoded_bytes[4] != 0x03 or decoded_bytes[6] != 0x04 or decoded_bytes[8] != 0x05 or decoded_bytes[10] != 0x06

                    for i in range(0, 11):
                        # Check if the byte is in order and not higher than 0x08
                        if settingscheck or decoded_bytes[i] > 0x08:
                            print("Invalid message structure")
                            break

                    # Call the function with your decoded bytes
                    apply_settings(decoded_bytes)

    # If we got here, we lost the connection. Go up to the top and start
    # advertising again and waiting for a connection.


    