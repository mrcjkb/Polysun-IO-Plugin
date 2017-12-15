package de.htw.berlin.io.csv.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;
import com.velasolaris.plugin.controller.spi.PluginControllerException;

public class MatWriterController extends AbstractWriterController {

	/** Path to the default plugin controller image. */
	public static final String DEF_IMGPATH = "plugin/images/controller_plugin.png";
	/** Path to the plugin controller image. */
	public static final String IMGPATH = "plugin/images/controller_mat.png";
	private static final int HOURS_PER_YEAR = 8760;
	private static final int SECONDS_PER_HOUR = 3600;
	private static final int MINUTES_PER_HOUR = 60;
	/** Number of columns to add to 2D array if it is too small to add new sensors */
	private static final int COLS_TO_ADD = 100000;
	
	/** The buffer for the sensor data */
	private double[][] mPolysunSensorData;
	/** The number of rows to initialize with */
	private int mNumRows;
	/** The number of columns to initialize with */
	private int mNumCols;
	/** The current column being written to */
	private int mCurrCol;
	
	public MatWriterController() {
		super();
	}

	@Override
	public String getName() {
		return "MAT Writer";
	}

	@Override
	public String getDescription() {
		return "Writes the sensor inputs to a matrix in a Matlab MAT file (starting at the beginning of the simulation)."
				+ " The MAT file is written at the end of the simulation. "
				+ " Hint: If a fixed time step size is used, set it with this controller to minimise memory issues."
				+ " Warning: Unlike the CSV Writer, this controller does not flush the buffered data until the simulation is terminated."
				+ " This may cause out of memory errors for low fixed time step sizes and large amounts of inputs.";
	}

	@Override
	public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters)
			throws PluginControllerException {
		List<Property> properties = initializePropertyList();
		String fileNameWithExtension = getDefaultFilePathAndName() + ".mat";
		properties.add(new Property(PATH_KEY, fileNameWithExtension, "The path to the MAT file (including file extension)"));
		return new PluginControllerConfiguration(properties, null, null, null, 0, MAX_NUM_GENERIC_SENSORS, 0, getPluginIconResource(), null);
	}

	@Override
	public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
		super.initialiseSimulation(parameters);
		List<Sensor> pluginsensors = getSensors();
		if (getFixedTimestep(parameters) == 0) {
			// Assume a value is written every minute.
			mNumCols = HOURS_PER_YEAR * MINUTES_PER_HOUR;
		} else {
			// Determine from fixed time step size how many values are written.
			mNumCols = HOURS_PER_YEAR * SECONDS_PER_HOUR;
		}
		mNumRows = 0;
		for (Sensor s : pluginsensors) {
			if (!s.isUsed()) {
				break;
			}
			mNumRows++;
		}
		if (getProp(TIMESTAMPSETTING_KEY).getInt() == ENABLE_TIMESTAMP) {
			mNumRows++; // Add additional column for time stamp
		}
		mPolysunSensorData = new double[mNumRows][mNumCols];
		mCurrCol = -1;
	}
	
	@Override
	public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
			boolean preRun, Map<String, Object> parameters) throws PluginControllerException {
		double weight = computeTimestepWeight(simulationTime);
		if (!preRun && status && isWriteTimestep(simulationTime)) {
			if (++mCurrCol >= mNumCols) { // Increase the buffer size
				mNumCols += COLS_TO_ADD;
				mPolysunSensorData = increaseCols(mPolysunSensorData, mNumCols);
			}
			int ct = 0;
			for (float s : sensors) {
				if (Float.isNaN(s)) {
					break;
				}
				mRunningSums[ct] += s * weight;
				mPolysunSensorData[ct][mCurrCol] = mRunningSums[ct];
				mRunningSums[ct++] = 0; // Reset running sum
			}
			if (getProp(TIMESTAMPSETTING_KEY).getInt() == ENABLE_TIMESTAMP) {
				mPolysunSensorData[ct++][mCurrCol] = simulationTime;
			}
		} else if (!preRun && status) {
			incrementRunningSums(sensors, weight);
		}
		setLastSimulationTime(simulationTime);
		return null;
	}
	
	@Override
	protected void flushAndClose() {
		try {
			if (++mCurrCol < mNumCols) {
				// Reduce the buffer size
				mPolysunSensorData = reduceCols(mPolysunSensorData, mCurrCol);
			}
			Collection<MLArray> mlSensorData = new ArrayList<MLArray>();
			mlSensorData.add(new MLDouble("sensors", mPolysunSensorData));
			try {
				new MatFileWriter(getFileName(), mlSensorData);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Saving the MAT file failed.", "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} finally {
				// Clean up
				mPolysunSensorData = null;
			}
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(null, "Failed to save MAT file: " + e.getClass().getCanonicalName(), "Error", JOptionPane.ERROR_MESSAGE);
		} finally {
			mPolysunSensorData = null;
		}
	}
	
	/**
	 * Attempts to load the custom 4diac plugin icon.
	 * @return A <code>String</code> representing the relative path to the icon. If loading the custom 4diac plugin icon fails, a <code>String</code>
	 * representing the relative path to the default plugin controller icon is returned.
	 */
	public static String getPluginIconResource() {
		if (ClassLoader.getSystemResource(IMGPATH) == null) {
			return DEF_IMGPATH;
		}
		return IMGPATH;
	}

	/**
	 * Deep copies a 2D double array to a 2D double array with a larger or smaller amount of columns
	 * @param aSource the source array
	 * @param newSize the new size
	 * @return the destination array
	 */
	public static double[][] increaseCols(double[][] aSource, int newSize) {
		double[][] aDestination = new double[aSource.length][newSize];
	    for (int i = 0; i < aSource.length; i++) {
	        System.arraycopy(aSource[i], 0, aDestination[i], 0, aSource[i].length);
	    }
	    return aDestination;
	}
	
	/**
	 * Deep copies a 2D double array to a 2D double array with a larger or smaller amount of columns
	 * @param aSource the source array
	 * @param newSize the new size
	 * @return the destination array
	 */
	public static double[][] reduceCols(double[][] aSource, int newSize) {
		double[][] aDestination = new double[aSource.length][newSize];
	    for (int i = 0; i < aSource.length; i++) {
	        System.arraycopy(aSource[i], 0, aDestination[i], 0, newSize);
	    }
	    return aDestination;
	}
}
