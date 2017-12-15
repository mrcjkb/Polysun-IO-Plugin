package de.htw.berlin.io.csv.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;

public abstract class AbstractWriterController extends AbstractPluginController {

	/** Key to the property specifying the file path and name */
	protected static final String PATH_KEY = "File path and name";
	/** Key to the property specifying a fixed time step size */
	private static final String FIXED_TIMESTEP_KEY = "Fixed timestep";
	/** Key to the property specifying whether to write data only at fixed time steps */
	private static final String ONLY_FIXED_WRITE_KEY = "Write data only at fixed time steps";
	/** Key for the timestamp option. */
	protected static final String TIMESTAMPSETTING_KEY = "Include simulation time stamp";
	/** Key for option to increment file names */
	private static final String INCREMENT_FILENAME_KEY = "Append number to file name that increments with each simulation";
	/** Path to the default plugin controller image. */
	public static final String DEF_IMGPATH = "plugin/images/controller_plugin.png";
	/** Maximum number of generic sensors that can be connected. */
	protected static final int MAX_NUM_GENERIC_SENSORS = 30;
	/** Only write data at fixed time steps */
	private static final int ONLY_FIXED_WRITE = 0;
	/** Index for enabled sending of time stamp. */
	protected static final int ENABLE_TIMESTAMP = 1;
	/** Index for incrementing file name set to disabled */
	protected static final int DONT_INCREMENT_FILENAME = 0;
	
	/** The fixed time step in s */
	private int mFixedTimeStep;
	/** Only write data for fixed time steps? */
	private boolean mOnlyFixedWrite;
	/** Increments with each successful simulation */
	private transient int mSimulationCount;
	/** Running sums used in case of writing only fixed time steps */
	protected transient float[] mRunningSums;
	/** simulationTime from the last call of control() */
	private transient int mLastSimulationTime;
	
	public AbstractWriterController() {
		super();
	}

	@Override
	public String getVersion() {
		return "2.1.0";
	}
	
	@Override
	public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
		super.initialiseSimulation(parameters);
		mRunningSums = new float[getNumUsedSensors()];
		if (getProp(FIXED_TIMESTEP_KEY).getInt() == 0) {
			mOnlyFixedWrite = false;
		} else {
			mOnlyFixedWrite = getProp(ONLY_FIXED_WRITE_KEY).getInt() == ONLY_FIXED_WRITE;
		}
	}
	
	@Override
	public void build(PolysunSettings polysunSettings, Map<String, Object> parameters) throws PluginControllerException {
		super.build(polysunSettings, parameters);
		mFixedTimeStep = getProperty(FIXED_TIMESTEP_KEY).getInt();
	}
	
	@Override
	public int getFixedTimestep(Map<String, Object> parameters) {
		return mFixedTimeStep;
	}
	
	@Override
	public List<String> getPropertiesToHide(PolysunSettings polysunSettings, Map<String, Object> parameters) {
		List<String> propertiesToHide = super.getPropertiesToHide(polysunSettings, parameters);
		if (getProp(FIXED_TIMESTEP_KEY).getInt() == 0) {
			propertiesToHide.add(ONLY_FIXED_WRITE_KEY);
		}
		return propertiesToHide;
	}
	
	@Override
	public void terminateSimulation(Map<String, Object> parameters) {
		incrementSimulationCount();
		flushAndClose();
	}
	
	/**
	 * @return A <code>List</code> of default properties.
	 */
	protected List<Property> initializePropertyList() {
		List<Property> properties = new ArrayList<Property>();
		properties.add(new Property(FIXED_TIMESTEP_KEY, 0, 0, 900, "The fixed time step in s"));
		properties.add(new Property(ONLY_FIXED_WRITE_KEY, new String[] { "yes", "no" }, ONLY_FIXED_WRITE, "This is automatically set to no if no fixed time step is set."));
		properties.add(new Property(TIMESTAMPSETTING_KEY, new String[] { "no" , "yes" }, ENABLE_TIMESTAMP, "Include the simulation time."));
		properties.add(new Property(INCREMENT_FILENAME_KEY, new String[] {"no", "yes"}, DONT_INCREMENT_FILENAME, "Use this option to prevent files from being overwritten when simulating multiple variants."));
		return properties;
	}
	
	/**
	 * @param the simulation time passed down from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @return <code>true</code> if the controller should write data
	 */
	protected boolean isWriteTimestep(int simulationTime) {
		return onlyWriteAtFixedTimesteps() ? simulationTime % getFixedTimestep(null) == 0 : true;
	}
	
	/**
	 * @return <code>true</code> if the controller should only write data at fixed time steps.
	 */
	protected boolean onlyWriteAtFixedTimesteps() {
		return mOnlyFixedWrite;
	}
	
	/**
	 * Performs actions so that the garbage collector can perform memory clean up and closes all files.
	 * This method should be called whenever the simulation is terminated (whether expected or unexpected).
	 */
	protected abstract void flushAndClose();

	/**
	 * @return number of simulations that have been run
	 */
	protected int getSimulationCount() {
		return mSimulationCount;
	}

	/**
	 * Increments the internal simulation counter
	 */
	private void incrementSimulationCount() {
		this.mSimulationCount += 1;
	}
	
	/**
	 * @return Name of file to be saved (including path)
	 */
	protected String getFileName() {
		String name = getProp(PATH_KEY).getString();
		if (getProp(INCREMENT_FILENAME_KEY).getInt() != DONT_INCREMENT_FILENAME) {
			// Add simulation count between name and extension
			int lastDot = name.lastIndexOf('.');
			name = name.substring(0, lastDot) + "_" + String.format("%03d", getSimulationCount()) + name.substring(lastDot);
		}
		return name;
	}
	
	/**
	 * @return Default file path and name (without extension), depending on the OS
	 */
	protected String getDefaultFilePathAndName() {
		String pathAndName = System.getProperty("user.home");
		if (SystemUtils.IS_OS_WINDOWS) {
			return pathAndName + "\\Desktop\\output";
		}
		// Linux
		return pathAndName + "/output";
	}
	
	/**
	 * @return The number of utilized sensors
	 */
	private int getNumUsedSensors() {
		int num = 0;
		List<Sensor> sensors = getSensors();
		for (Sensor s : sensors) {
			if (s.isUsed()) {
				num++;
			}
		}
		return num;
	}

	protected int getLastSimulationTime() {
		return mLastSimulationTime;
	}

	protected void setLastSimulationTime(int mLastSimulationTime) {
		this.mLastSimulationTime = mLastSimulationTime;
	}
	
	/**
	 * @param simulationTime simulation time in s
	 * @return the weight for data to be saved depending on the time step size
	 */
	protected double computeTimestepWeight(int simulationTime) {
		return onlyWriteAtFixedTimesteps() ? (double) (simulationTime - getLastSimulationTime()) / getFixedTimestep(null) : 1;
	}
	
	/**
	 * Increments the array of running sums
	 * @param sensors sensor data from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @param weight value returned by {@link #computeTimestepWeight(int)}
	 */
	protected void incrementRunningSums(float[] sensors, double weight) {
		int ct = 0;
		for (float s : sensors) {
			if (Float.isNaN(s)) {
				break;
			}
			mRunningSums[ct++] += s * weight;
		}
	}
}
