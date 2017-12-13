package de.htw.berlin.io.csv.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;
import com.velasolaris.plugin.controller.spi.PluginControllerException;

public class CSVWriterController extends AbstractWriterController {

	private static final String DELIMITER_KEY = "Delimiter";
	private static final String INCLUDE_HEADERS_KEY = "Include headers";
	/** Path to the plugin controller image. */
	public static final String IMGPATH = "plugin/images/controller_csv.png";
	private static final int MAX_NUM_GENERIC_SENSORS = 30;
	private static final int INCLUDE_HEADERS = 0;

	private BufferedWriter mBuffer = null;
	private Writer mWriter = null;
	private File mFile = null;

	public CSVWriterController() {
		super();
	}

	@Override
	public String getName() {
		return "CSV Writer";
	}
	
	@Override
	public String getDescription() {
		return "Writes the sensor inputs to a CSV file (starting at the beginning of the simulation).";
	}

	@Override
	public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters)
			throws PluginControllerException {
		List<Property> properties = initializePropertyList();
		String fileNameWithExtension = getDefaultFilePathAndName() + ".csv";
		properties.add(new Property(PATH_KEY, fileNameWithExtension, "The full path to the CSV file (including file extension)."));
		properties.add(new Property(DELIMITER_KEY, ";", "The delimiter used for separating values."));
		properties.add(new Property(INCLUDE_HEADERS_KEY, new String[] { "yes", "no" }, INCLUDE_HEADERS, "Include the CSV headers in the file"));
		return new PluginControllerConfiguration(properties, null, null, null, 0, MAX_NUM_GENERIC_SENSORS, 0, getPluginIconResource(), null);
	}

	@Override
	public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
			boolean preRun, Map<String, Object> parameters) throws PluginControllerException {
		if (preRun && mFile != null) {
			flushAndClose();
		}
		if (!preRun && status && isWriteTimestep(simulationTime)) {
			if (simulationTime == 0 || mFile == null) {
				mFile = new File(getFileName());
				if (mFile.exists()) {
					if (!mFile.delete()) {
						flushAndClose();
						if (!mFile.delete()) {
							throw new PluginControllerException(getName() + "Failed to delete old CSV file.");
						}
					}
				}
				if (!mFile.exists()) {
					try {
						mFile.createNewFile();
					} catch (IOException e) {
						throw new PluginControllerException(getName() + ": Error creating file.", e);
					}
				}
				try {
					try {
						mWriter = new FileWriter(mFile);
						mBuffer = new BufferedWriter(getWriter());
					} catch (IOException e) {
						throw new PluginControllerException(getName() + ": Error opening file.", e);
					}
					if (getProp(INCLUDE_HEADERS_KEY).getInt() == INCLUDE_HEADERS) {
						try {
							if (getProp(TIMESTAMPSETTING_KEY).getInt() == ENABLE_TIMESTAMP) {
								getBuffer().write("Simulation time [s]");
								writeDelimiter();
							}
							List<Sensor> pluginsensors = getSensors();
							for (Sensor s : pluginsensors) {
								if (!s.isUsed()) {
									break;
								}
								getBuffer().write(s.getName() + "[" + s.getUnit() + "]");
								writeDelimiter();
							}
							getBuffer().newLine();
						} catch (IOException e) {
							throw new PluginControllerException(getName() + ": Error writing headers to file.", e);
						}
					}
				} catch (PluginControllerException e) {
					flushAndClose(); // Close file before throwing exception
					throw e;
				}
			}
			try {
				if (getProp(TIMESTAMPSETTING_KEY).getInt() == ENABLE_TIMESTAMP) {
					getBuffer().write(Integer.toString(simulationTime));
					writeDelimiter();
				}
				int ct = 0;
				for (float s : sensors) {
					if (Float.isNaN(s)) {
						break;
					}
					// Weigh against time step size if averaging over fixed time step size
					double weight = onlyWriteAtFixedTimesteps() ? (simulationTime - getLastSimulationTime()) / getFixedTimestep(null) : 1;
					mRunningSums[ct] += s * weight;
					getBuffer().write(Float.toString(mRunningSums[ct]));
					mRunningSums[ct++] = 0; // Reset running sum
					writeDelimiter();
				}
				getBuffer().newLine();
			} catch (IOException e) {
				flushAndClose();
				throw new PluginControllerException(getName() + ": Error writing data to file.", e);
			}
		} else if (!preRun && status ) {
			int ct = 0;
			for (float s : sensors) {
				if (Float.isNaN(s)) {
					break;
				}
				mRunningSums[ct++] += s * (simulationTime - getLastSimulationTime()) / getFixedTimestep(null);
			}
		}
		setLastSimulationTime(simulationTime);
		return null;
	}

	/**
	 * Writes a delimiter to the CSV.
	 * @throws IOException
	 */
	private void writeDelimiter() throws IOException {
		mBuffer.write(getProp(DELIMITER_KEY).getString());
	}

	@Override
	protected void flushAndClose() {
		try {
			getBuffer().flush();
			getBuffer().close();
			getWriter().close();
			mFile = null;
		} catch (IOException e) {
			// Ignore. File was probably already closed.
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
	 * @return The BufferedWriter used for writing the CSV file
	 */
	private BufferedWriter getBuffer() {
		return mBuffer;
	}

	/**
	 * @return The FileWriter used for writing the CSV file
	 */
	private Writer getWriter() {
		return mWriter;
	}
}
