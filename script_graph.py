import matplotlib.pyplot as plt
import numpy as np


def meanFile(rate, pwdLen, smart):
    path = ("measures/" + "measure-smart" + str(smart) + "-rate" + str(rate) + "-pwdLen" + str(pwdLen)+ ".csv")
    file = open(path, "r")
    line = file.readline()
    split = line.split(",")
    l = [int(el) for el in split]
    mean = np.mean(l)
    return mean


# Rate variation
rates = [5, 25, 50, 75, 100]
avgRespDumb = [meanFile(rates[i], 3, 0) for i in range(len(rates))]
avgRespSmart = [meanFile(rates[i], 3, 1) for i in range(len(rates))]
plt.plot(rates, avgRespDumb)
plt.plot(rates, avgRespSmart)
plt.title("Average response time VS request rate for 20kB files")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (s)")
plt.show()

# Password size variation
pwdLengths = [1, 2, 3, 4, 5, 6]
avgRespDumb = [meanFile(25, pwdLengths[i], 0) for i in range(len(pwdLengths))]
avgRespSmart = [meanFile(25, pwdLengths[i], 1) for i in range(len(pwdLengths))]
plt.plot(pwdLengths, avgRespDumb)
plt.plot(rates, avgRespSmart)
plt.title("Average response time VS password size for 20kB files")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (s)")
plt.show()