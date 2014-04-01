import base64
import numpy
import matplotlib.pyplot as plt
import cStringIO

x = numpy.linspace(-15,15,100)
y = numpy.sin(x)/x
plt.plot(x,y)
plt.plot(x,y,'co')
plt.plot(x,2*y,x,3*y)

# save to document:
ram = cStringIO.StringIO()
plt.savefig(ram, format="svg")
ram.seek(0)
print "data:image/svg+xml;base64," + base64.b64encode(ram.read())
