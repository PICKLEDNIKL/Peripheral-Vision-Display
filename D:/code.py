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
from adafruit_ble.characteristics import Characteristic
from adafruit_ble.services import Service
from adafruit_ble.uuid import StandardUUID
from rainbowio import colorwheel
import neopixel
import binascii
from adafruit_debouncer import Debouncer

led = digitalio.DigitalInOut(board.LED)
led.direction = digitalio.Direction.OUTPUT

ble = BLERadio()
uart = UARTService()

advertisement = ProvideServicesAdvertisement(uart)
i2c = board.I2C()  # uses board.SCL and board.SDA
glasses = LED_Glasses(i2c, allocate=adafruit_is31fl3741.MUST_BUFFER)
glasses.show() #clear residual data
glasses.global_current = 20
# glasses.set_led_scaling(100)

brightness_levels = [5, 15, 30, 75, 125, 175, 255]

left_ring = glasses.left_ring

right_ring = glasses.right_ring

pixel_pin = board.NEOPIXEL
pixels = neopixel.NeoPixel(pixel_pin, 1, brightness=0.1, auto_write=False)

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

def apply_settings(decoded_bytes: bytes) -> None:
    """decodes bytes into settings and colors and applies them to the glasses

    Args:
        decoded_bytes (bytes): settings and colors in bytes
    """    

    global notifcolor, leftcolor, rightcolor, strcolor, turncolor, motionboolean

    # Define the mappings from byte values to settings and colors
    settings_map = {1: "NOTIF", 2: "LEFT", 3: "RIGHT", 4: "STR", 5: "TURN", 6: "LED", 7: "BRIGHT"}
    colors_map = {1: RED, 2: PURPLE, 3: BLUE, 4: GREEN, 5: YELLOW, 6: ORANGE, 7: True, 8: False}

    # Iterate over the bytes in pairs
    for i in range(0, len(decoded_bytes), 2):
        # Get the setting and color from the byte values
        setting = settings_map.get(decoded_bytes[i], "UNKNOWN")
        if setting != "BRIGHT":
            color = colors_map.get(decoded_bytes[i + 1], "UNKNOWN")
        else:
            brightness = decoded_bytes[i + 1]

        # Apply the setting
        # print(f"Applying setting {setting} with color {color}")

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
        elif setting == "BRIGHT":
            adjust_brightness(brightness)
    pixels.fill((255, 255, 0))
    pixels.show()
    # time.sleep(1)
    # pixels.fill((0, 0, 0))
    # pixels.show()
    return

def update_left_str():
    for i in range(21, 24):
        left_ring[i] = strcolor
        glasses.show()
        time.sleep(0.025)
        yield
    for i in range(0, 3):
        left_ring[i] = strcolor
        glasses.show()
        time.sleep(0.025)
        yield
def update_right_str():
    for i in range(3, -1, -1):
        right_ring[i] = strcolor
        print("rightring -1 ",i)
        glasses.show()
        time.sleep(0.025)
        yield
    for x in range(23, 21, -1):
        right_ring[x] = strcolor
        print(x)
        glasses.show()
        time.sleep(0.025)
        yield

def reset_left_str():
    for i in range(21, 24):
        left_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield
    for i in range(0, 3):
        left_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield
def reset_right_str():
    for i in range(3, -1, -1):
        right_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield
    for i in range(23, 21, -1):
        right_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield

def update_left_turn():
    for i in range(9, 17):
        left_ring[i] = turncolor
        glasses.show()
        time.sleep(0.025)
        yield
def update_right_turn():
    for i in range(15, 8, -1):
        right_ring[i] = turncolor
        glasses.show()
        time.sleep(0.025)
        yield

def reset_left_turn():
    for i in range(9, 17):
        left_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield
def reset_right_turn():
    for i in range(15, 8, -1):
        right_ring[i] = 0
        glasses.show()
        time.sleep(0.025)
        yield

def adjust_brightness(brightness):

    # Set the brightness of the LEDs
    glasses.set_led_scaling(brightness_levels[brightness])

    # Show the changes
    glasses.show()
    return

while True:
    if ble.advertising:
        ble.stop_advertising()
    # if not ble.advertising:
    ble.start_advertising(advertisement)
    while not ble.connected:
        # Light up LEDs with blue and red for advertising connection

        # Light LEDs on the glasses with RED and microcontroller with BLUE
        for i in range(18, 21):
            right_ring[i] = RED
        for i in range(4, 7):
            left_ring[i] = RED
        pixels.fill((0, 0, 255))
        pixels.show()
        glasses.show()

        time.sleep(0.5)

        # Light LEDs on the glasses with BLUE and microcontroller with RED
        for i in range(18, 21):
            right_ring[i] = BLUE
        for i in range(4, 7):
            left_ring[i] = BLUE
        pixels.fill((255, 0, 0))
        pixels.show()
        glasses.show()

        time.sleep(0.5)

    # Light up LEDs to indicate connection
    for count in range(0, 2):

        for i in range(0, 24):
            right_ring[i] = GREEN
        for i in range(0, 24):
            left_ring[i] = GREEN
        pixels.fill((0, 255, 0))
        pixels.show()
        glasses.show()
        time.sleep(0.25)

        for i in range(0, 24):
            right_ring[i] = 0
        for i in range(0, 24):
            left_ring[i] = 0
        glasses.show() 

    while ble.connected:

        if uart.in_waiting:
            data = uart.read(32)

            if data is not None:
                message = data.decode("utf-8")
                print("message received")
                print(message)

                # NOTIF
                if message.startswith('NOTIF'):
                    if motionboolean:
                        for i in range(0, 19, 6):
                            left_ring[i] = notifcolor
                            right_ring[i] = notifcolor
                            glasses.show() 
                            time.sleep(0.15)
                        for i in range(0, 19, 6):
                            left_ring[i] = 0
                            right_ring[i] = 0
                            glasses.show() 
                            time.sleep(0.15)

                    elif not motionboolean:
                        for i in range(0, 19, 6):
                            left_ring[i] = notifcolor
                            right_ring[i] = notifcolor
                        glasses.show() 
                        time.sleep(1)
                        for i in range(0, 19, 6):
                            left_ring[i] = 0
                            right_ring[i] = 0
                        glasses.show() 
                        time.sleep(1)

                # LEFT
                elif message.startswith('LEFT'):
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
                                left_ring[i] = leftcolor
                            glasses.show()
                            time.sleep(0.5)
                            for i in range(14, 23):
                                left_ring[i] = 0
                            glasses.show()
                            time.sleep(0.5)

                # RIGHT
                elif message.startswith('RIGHT'):
                    for count in range(0, 2):
                        if motionboolean:
                            # light up the right side of the right ring
                            for i in range(2, 10):
                                right_ring[i] = rightcolor # blue in hex rgb
                                glasses.show()
                                time.sleep(0.05)
                            for i in range(2, 10):
                                right_ring[i] = 0
                                glasses.show()
                                time.sleep(0.05)
                        elif not motionboolean:
                            # light up the right side of the right ring
                            for i in range(2, 10):
                                right_ring[i] = rightcolor # blue in hex rgb
                            glasses.show()
                            time.sleep(0.5)
                            for i in range(2, 10):
                                right_ring[i] = 0
                            glasses.show()
                            time.sleep(0.5)

                #STR
                elif message.startswith('STR'):
                    for count in range(0, 2):
                        if motionboolean:
                            left = update_left_str()
                            right = update_right_str()
                            while True:
                                try:
                                    next(right)
                                    next(left)
                                except StopIteration:
                                    break

                            # Repeat for resetting LEDs
                            left = reset_left_str()
                            right = reset_right_str()
                            while True:
                                try:
                                    next(right)
                                    next(left)
                                except StopIteration:
                                    break

                        elif not motionboolean:
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
                            time.sleep(0.5)
                # TURN
                elif message.startswith('TURN'):
                    #led from the bottom - MAYBE MAKE IT TO DO AN ANIMATION LATER ON
                    for count in range(0, 2):
                        if motionboolean:
                            left = update_left_turn()
                            right = update_right_turn()
                            while True:
                                try:
                                    next(right)
                                    next(left)
                                except StopIteration:
                                    break

                            # Repeat for resetting LEDs
                            left = reset_left_turn()
                            right = reset_right_turn()
                            while True:
                                try:
                                    next(right)
                                    next(left)
                                except StopIteration:
                                    break

                        elif not motionboolean:
                            # light up bottom of left ring
                            for i in range(9, 16):
                                left_ring[i] = turncolor
                                right_ring[i] = turncolor
                            glasses.show()

                            # reset leds
                            time.sleep(0.5)
                            for i in range(9, 16):
                                left_ring[i] = 0
                                right_ring[i] = 0
                            glasses.show()
                            time.sleep(0.5)
                else:
                    # Check size of message. If the message has a length less than 16, this message is not for setting configuration.
                    # If the size of the message is more than 16, there was likely an issue with messages being sent too quickly. 
                    if len(message) < 17 and len(message) > 5:
                        print("Message length is incorrect")
                        break
                    elif len(message) > 17:
                        message = message[:17]

                    if len(message) == 17:
                        # Decode the message from base64
                        decoded_bytes = binascii.a2b_base64(message)
                        print(decoded_bytes)

                        # Check the structure of the message
                        settingscheck = decoded_bytes[0] != 0x01 or decoded_bytes[2] != 0x02 or decoded_bytes[4] != 0x03 or decoded_bytes[6] != 0x04 or decoded_bytes[8] != 0x05 or decoded_bytes[10] != 0x06
                        if decoded_bytes[11] in (0x07, 0x08):
                            booleancheck = False
                        else:
                            booleancheck = True
                        
                        for i in range(0, 11):
                            # Check if the byte is in order and not higher than 0x08
                            if (settingscheck) or (booleancheck) or (decoded_bytes[i] > 0x08):
                                print("Invalid message structure")
                                break
                            else:
                                # Call the function with your decoded bytes
                                apply_settings(decoded_bytes)  
                                break
                    # If the message is equal to 5, the message is for brightness settings.
                    elif len(message) == 5:
                        decoded_bytes = binascii.a2b_base64(message)
                        print(decoded_bytes)
                        brightnesscheck = decoded_bytes[0] != 0x07
                        if brightnesscheck:
                            print("Invalid brightness message")
                            break
                        else:
                            apply_settings(decoded_bytes)
                            break

    # If we got here, we lost the connection. Go up to the top and start
    # advertising again and waiting for a connection.


