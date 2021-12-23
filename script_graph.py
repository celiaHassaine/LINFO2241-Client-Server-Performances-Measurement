import matplotlib.pyplot as plt
import numpy as np


def meanFile(rate, pwdLen, smart, evolution):
    path = ("measures/" + evolution+ "/measure-smart" + str(smart) + "-rate" + str(rate) + "-pwdLen" + str(pwdLen)+ ".csv")
    file = open(path, "r")
    line = file.readline()
    split = line.split(",")
    del split[-1]
    print(split)
    l = [int(el) for el in split]
    mean = np.mean(l)
    return mean


# ---- Rate variation ----
rates = [1, 2, 3, 4, 5, 25, 50, 75, 100]

# DUMB
avgRespDumb3 = [meanFile(rates[i], 3, 0, "rate evolution") for i in range(len(rates))]
avgRespDumb5 = [meanFile(rates[i], 5, 0, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespDumb3, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespDumb5, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# SMART
avgRespSmart3 = [meanFile(rates[i], 3, 1, "rate evolution") for i in range(len(rates))]
avgRespSmart5 = [meanFile(rates[i], 5, 1, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespDumb3, marker='.')
plt.plot(rates, avgRespSmart3, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespSmart5, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# ---- Password size variation ----
pwdLengths = [1, 2, 3, 4, 5, 6]

# DUMB
avgRespDumb5 = np.log(np.array([meanFile(5, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))]))
avgRespDumb25 = np.log(np.array([meanFile(25, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))]))
print(avgRespDumb25)

plt.plot(pwdLengths, avgRespDumb5, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("log(Average response time (ms))")
plt.show()

plt.plot(pwdLengths, avgRespDumb25, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("log(Average response time (ms))")
plt.show()

# SMART
avgRespSmart5 = [meanFile(5, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]
avgRespSmart25 = [meanFile(5, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]

plt.plot(pwdLengths, avgRespSmart5, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

plt.plot(pwdLengths, avgRespSmart25, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

