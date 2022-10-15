

# Polysun CSV Writer Plugin

> [![Maintenance](https://img.shields.io/badge/Maintained%3F-no-red.svg)](https://bitbucket.org/lbesson/ansi-colors)
>
> I will gladly accept/review PRs.

Open source Polysun plugin with a set of plugin controllers that can be used for communication in co-simulations with IEC 61499 applications running on 4diac-RTE (FORTE).


# Use
Requirements for use:

  - Polysun (http://www.velasolaris.com/)

To load the plugin into Polysun, copy the file IOPlugin.jar from ..\target\dist\ into Polysun's plugins folder (usually C:\Users\..\Polysun\plugins\) and launch Polysun.

It contains the following plugin controllers:

	- CSV Writer: Writes the sensor inputs into a delimited CSV file.
	- MAT Writer: Writes the sensor inputs as a matrix in a Matlab MAT file.

Both plugins have the option to write the simulation time (in s since the beginning of the year) as a time stamp. In the case of the MAT Writer, the simulation time is written to the last row.
For increased performance, the MAT writer writes the inputs to a matrix as row vectors. If a time stamp is written, it would be accessed via sensors(end,:);
To extract input no. x from the matrix, use the following indexing: sensors(x,:)

NOTE: There is currently a bug in Polysun preventing the correct plugin icon from being loaded. This is not a severe issue, as the default plugin icon is loaded instead.
If you would like to load the CSVWriter plugin icon, copy the file Icons.jar from Polysuns' pictures path to the desktop (make a backup of the file first).
You will find the file in: C:\Program Files (x86)\Polysun\pictures or C:\Program Files\Polysun\pictures
Rename it to Icons.zip (make sure to show file extensions in Windows Explorer) and add the image "controller_csv.png" to the archive.
You can find the image in: ..\src\main\resources\plugin\images\
Then rename the archive to Icons.jar and copy it back to its original location.


# For developers:
The following libraries are required for development:

	- PolysunPluginDevelopmentKit (ships with Polysun)
	- JMatIO (for the MatWriterController) https://github.com/gradusnikov/jmatio
