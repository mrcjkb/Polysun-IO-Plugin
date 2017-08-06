package de.htw.berlin.io.csv.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PolysunSettings;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;

public abstract class AbstractWriterController extends AbstractPluginController {

	/** Key to the property specifying the file path and name */
	protected static final String PATH_KEY = "File path and name";
	/** Key to the property specifying a fixed time step size */
	private static final String FIXED_TIMESTEP_KEY = "Fixed timestep";
	/** Key to the property specifying whether to write data only at fixed time steps */
	private static final String ONLY_FIXED_WRITE_KEY = "Write data only at fixed time steps";
	/** Key for the timestamp option. */
	protected static final String TIMESTAMPSETTING_KEY = "Include simulation time stamp";
	/** Path to the default plugin controller image. */
	public static final String DEF_IMGPATH = "plugin/images/controller_plugin.png";
	/** Maximum number of generic sensors that can be connected. */
	protected static final int MAX_NUM_GENERIC_SENSORS = 30;
	/** Only write data at fixed time steps */
	private static final int ONLY_FIXED_WRITE = 0;
	/** Index for enabled sending of time stamp. */
	protected static final int ENABLE_TIMESTAMP = 1;
	
	/** The fixed time step in s */
	private int mFixedTimeStep;
	/** Only write data for fixed time steps? */
	private boolean mOnlyFixedWrite;
	
	public AbstractWriterController() {
		super();
	}

	@Override
	public String getVersion() {
		return "1.3.0";
	}
	
	@Override
	public void initialiseSimulation(Map<String, Object> parameters) throws PluginControllerException {
		super.initialiseSimulation(parameters);
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
		return properties;
	}
	
	/**
	 * @param the simulation time passed down from the {@link #control(int, boolean, float[], float[], float[], boolean, Map)} method.
	 * @return <code>true</code> if the controller should write data
	 */
	protected boolean isWriteTimestep(int simulationTime) {
		return onlyWriteAtFixedTimesteps() ? simulationTime % getFixedTimestep(null) == 0 : false;
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
}
