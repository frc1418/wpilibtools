#!/usr/bin/env python3

import time

for i in range(1000):
    time.sleep(0.03)
    print("Blip. -------------------------------------------- {:> 5d}".format(i))
