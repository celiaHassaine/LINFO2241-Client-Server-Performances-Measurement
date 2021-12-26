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


"""# ---- Rate variation ----
rates = [1, 2, 3, 4, 5, 25, 50, 75, 100]

# DUMB
avgRespDumb3R = [meanFile(rates[i], 3, 0, "rate evolution") for i in range(len(rates))]
avgRespDumb5R = [meanFile(rates[i], 5, 0, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespDumb3R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespDumb5R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# SMART
avgRespSmart3R = [meanFile(rates[i], 3, 1, "rate evolution") for i in range(len(rates))]
avgRespSmart5R = [meanFile(rates[i], 5, 1, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespSmart3R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespSmart5R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# ---- Password size variation ----
pwdLengths = [1, 2, 3, 4, 5, 6]

# DUMB
avgRespDumb5P = np.log(np.array([meanFile(5, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))]))
avgRespDumb25P = np.log(np.array([meanFile(25, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))]))

plt.plot(pwdLengths, avgRespDumb5P, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("log(Average response time (ms))")
plt.show()

plt.plot(pwdLengths, avgRespDumb25P, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("log(Average response time (ms))")
plt.show()

# SMART
avgRespSmart5P = [meanFile(5, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]
avgRespSmart25P = [meanFile(5, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]

plt.plot(pwdLengths, avgRespSmart5P, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

plt.plot(pwdLengths, avgRespSmart25P, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()


# ---- DUMB VS. SMART ----
# RATE
plt.plot(rates, avgRespDumb3R, marker='.', label="Dumb")
plt.plot(rates, avgRespSmart3R, marker='.', label="Smart")
plt.legend(loc="upper left")
plt.show()

plt.plot(rates, avgRespDumb5R, marker='.', label="Dumb")
plt.plot(rates, avgRespSmart5R, marker='.', label="Smart")
plt.legend(loc="upper left")
plt.show()

# PWSIZE
plt.plot(pwdLengths, avgRespDumb5P, marker='.', label="Dumb")
plt.plot(pwdLengths, avgRespSmart5P, marker='.', label="Smart")
plt.legend(loc="upper left")
plt.show()

plt.plot(pwdLengths, avgRespDumb25P, marker='.', label="Dumb")
plt.plot(pwdLengths, avgRespSmart25P, marker='.', label="Smart")
plt.legend(loc="upper left")
plt.show()"""


# ---- Rate variation ----
rates = [1, 20, 40, 60, 80, 100]

# DUMB
avgRespDumb3R = [meanFile(rates[i], 3, 0, "rate evolution") for i in range(len(rates))]
avgRespDumb5R = [meanFile(rates[i], 5, 0, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespDumb3R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespDumb5R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 DUMB")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# SMART
avgRespSmart3R = [meanFile(rates[i], 3, 1, "rate evolution") for i in range(len(rates))]
avgRespSmart5R = [meanFile(rates[i], 5, 1, "rate evolution") for i in range(len(rates))]

plt.plot(rates, avgRespSmart3R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=3 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()
plt.plot(rates, avgRespSmart5R, marker='.')
plt.title("Average response time VS request rate for 20kB files with pwdLen=5 SMART")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.show()

# ---- Password size variation ----
pwdLengths = [3, 4, 5, 6]


# DUMB
avgRespDumb20P = np.array([meanFile(20, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))])
avgRespDumb80P = np.array([meanFile(80, pwdLengths[i], 0, "length evolution") for i in range(len(pwdLengths))])

plt.plot(pwdLengths, avgRespDumb20P, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

plt.plot(pwdLengths, avgRespDumb80P, marker='.')
plt.title("Average response time VS password size for 20kB files DUMB")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

# SMART
avgRespSmart20P = [meanFile(20, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]
avgRespSmart80P = [meanFile(80, pwdLengths[i], 1, "length evolution") for i in range(len(pwdLengths))]

plt.plot(pwdLengths, avgRespSmart20P, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()

print("avg" + str(avgRespSmart80P))
plt.plot(pwdLengths, avgRespSmart80P, marker='.')
plt.title("Average response time VS password size for 20kB files SMART")
plt.xlabel("Password size (# char)")
plt.ylabel("Average response time (ms)")
plt.show()


# ---- DUMB VS. SMART ----
# RATE
plt.title("Average Response time (ms) VS rate (dumb and smart); pwdLen=3")
plt.plot(rates, avgRespDumb3R, marker='.', label="Dumb")
plt.plot(rates, avgRespSmart3R, marker='.', label="Smart")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.legend(loc="upper left")
plt.show()

plt.title("Average Response time (ms) VS rate (dumb and smart)")
plt.plot(rates, avgRespDumb5R, marker='.', label="Dumb, pwdLen=5")
plt.plot(rates, avgRespSmart5R, marker='.', label="Smart, pwdLen=5")
plt.xlabel("Rate (# requests/s)")
plt.ylabel("Average response time (ms)")
plt.legend(loc="upper left")
plt.show()
print("mean dumb :",avgRespDumb5R[-1])
print("mean smart :",avgRespSmart5R[-1])
perc1 = (avgRespDumb5R[-1]-avgRespSmart5R[-1])/avgRespDumb5R[-1]
perc2 = (avgRespDumb5R[-2]-avgRespSmart5R[-2])/avgRespDumb5R[-2]
perc3 = (avgRespDumb5R[-3]-avgRespSmart5R[-3])/avgRespDumb5R[-3]
perc4 = (avgRespDumb5R[-4]-avgRespSmart5R[-4])/avgRespDumb5R[-4]
print(perc1)
print(perc2)
print(perc3)
print(perc4)


# PWSIZE
plt.title("Average Response time (ms) VS pwdLength (dumb and smart); rate=20")
plt.plot(pwdLengths, avgRespDumb20P, marker='.', label="Dumb, rate=20")
plt.plot(pwdLengths, avgRespSmart20P, marker='.', label="Smart, rate=20")
plt.xlabel("Pwd length (# char)")
plt.ylabel("Average response time (ms)")
plt.legend(loc="upper left")
plt.show()

plt.title("Average Response time (ms) VS pwdLength (dumb and smart); rate=80")
plt.plot(pwdLengths, avgRespDumb80P, marker='.', label="Dumb")
plt.plot(pwdLengths, avgRespSmart80P, marker='.', label="Smart")
plt.xlabel("Pwd length (# char)")
plt.ylabel("Average response time (ms)")
plt.legend(loc="upper left")
plt.show()
print(avgRespSmart80P)

# BONUS ALL
plt.title("Rate 20 and rate 80")
plt.plot(pwdLengths, avgRespDumb20P, marker='.', label="Dumb, rate=20")
plt.plot(pwdLengths, avgRespSmart20P, marker='.', label="Smart, rate=20")
plt.plot(pwdLengths, avgRespDumb80P, '--', marker='.', label="Dumb, rate=80")
plt.plot(pwdLengths, avgRespSmart80P, '--', marker='.', label="Smart, rate=80")
plt.legend(loc="upper left")
plt.show()


def mu(avgResp, rate, m):
    return (1+(rate*avgResp))/(m*avgResp)

mu4 = mu(avgRespDumb80P, 80, 12)
print("mu last scenario", mu4)



