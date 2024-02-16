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


RED = 0xFF0000      # red in hex rgb
BLUE = 0x0000FF     # blue in hex rgb
GREEN = 0x00FF00    # green in hex rgb
YELLOW = 0xFFFF00   # yellow in hex rgb

while True:
    ble.start_advertising(advertisement)
    while not ble.connected:
        #check if trying to connect
        led.value = True
        time.sleep(0.5)
        led.value = False
        time.sleep(0.5)
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
                if message.startswith('NOTIF'):
                    # for count in range(0, 2):
                    # light up all of left ring
                    # for i in range(0, 24):
                    left_ring[0] = YELLOW
                    left_ring[6] = YELLOW
                    left_ring[12] = YELLOW
                    left_ring[18] = YELLOW

                    # light up all of right ring
                    # for i in range(0, 24):
                    right_ring[0] = YELLOW
                    right_ring[6] = YELLOW
                    right_ring[12] = YELLOW
                    right_ring[18] = YELLOW
                    glasses.show()

                    # reset leds
                    time.sleep(0.5)
                    for i in range(0, 24):
                        left_ring[i] = 0
                        right_ring[i] = 0
                    glasses.show()
                    time.sleep(0.25)
                # LEFT
                elif message.startswith('LEFT'):
                    for count in range(0, 2):
                        # light up the left side of the left ring
                        for i in range(14, 23):
                            left_ring[i] = 0x0000FF # blue in hex rgb
                        glasses.show()

                        # reset leds
                        time.sleep(0.5)
                        for i in range(0, 24):
                            left_ring[i] = 0
                        glasses.show()
                        time.sleep(0.25)
                # RIGHT
                elif message.startswith('RIGHT'):
                    for count in range(0, 2):
                        # light up the right side of the right ring
                        for i in range(2, 10):
                            right_ring[i] = 0x0000FF # blue in hex rgb
                        glasses.show()

                        # reset leds
                        time.sleep(0.5)
                        for i in range(0, 24):
                            right_ring[i] = 0
                        glasses.show()
                        time.sleep(0.25)

                #STR
                elif message.startswith('STR'):
                    for count in range(0, 2):
                        # light up top of left ring
                        for i in range(0, 3):
                            left_ring[i] = GREEN
                        for i in range(21, 24):
                            left_ring[i] = GREEN

                        # light up top of right ring
                        for i in range(0, 4):
                            right_ring[i] = GREEN
                        for i in range(22, 24):
                            right_ring[i] = GREEN
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
                            left_ring[i] = RED

                        # light up bottom of right ring
                        for i in range(9, 16):
                            right_ring[i] = RED
                        glasses.show()

                        # reset leds
                        time.sleep(0.5)
                        for i in range(0, 24):
                            left_ring[i] = 0
                            right_ring[i] = 0
                        glasses.show()
                        time.sleep(0.25)

                else:
                    print("unknown message received:")
                    print(message)

    # If we got here, we lost the connection. Go up to the top and start
    # advertising again and waiting for a connection.
