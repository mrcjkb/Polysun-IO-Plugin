package de.htw.berlin.io.csv.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.velasolaris.plugin.controller.spi.AbstractPluginController;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration;
import com.velasolaris.plugin.controller.spi.PluginControllerException;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Property;
import com.velasolaris.plugin.controller.spi.PluginControllerConfiguration.Sensor;

public class CSVWriterController extends AbstractPluginController {

	private static final String PATH_KEY = "File path and name";
	private static final String DELIMITER_KEY = "Delimiter";
	private static final String INCLUDE_HEADERS_KEY = "Include headers";
	/** Path to the default plugin controller image. */
	public static final String DEF_IMGPATH = "plugin/images/controller_plugin.png";
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
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return "Writes the sensor inputs to a CSV file (starting at the beginning of the simulation).";
	}

	@Override
	public PluginControllerConfiguration getConfiguration(Map<String, Object> parameters)
			throws PluginControllerException {
		List<Property> properties = new ArrayList<>();
		String path = System.getProperty("user.home") + "\\Desktop\\output.csv";
		properties.add(new Property(PATH_KEY, path, "The host name (e.g., the IP address) of the function block this plugin connects to."));
		properties.add(new Property(DELIMITER_KEY, ";", "The delimiter used for separating values."));
		properties.add(new Property(INCLUDE_HEADERS_KEY, new String[] { "yes", "no" }, INCLUDE_HEADERS, "Include the CSV headers in the file"));
		return new PluginControllerConfiguration(properties, null, null, null, 0, MAX_NUM_GENERIC_SENSORS, 0, getPluginIconResource(), null);
	}

	@Override
	public int[] control(int simulationTime, boolean status, float[] sensors, float[] controlSignals, float[] logValues,
			boolean preRun, Map<String, Object> parameters) throws PluginControllerException {
		if (!preRun && status) {
			if (mFile == null) {
				mFile = new File(getProp(PATH_KEY).getString());
				if (mFile.exists()) {
					mFile.delete();
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
						mBuffer = new BufferedWriter(mWriter);
					} catch (IOException e) {
						throw new PluginControllerException(getName() + ": Error opening file.", e);
					}
					if (getProp(INCLUDE_HEADERS_KEY).getInt() == INCLUDE_HEADERS) {
						try {
							getBuffer().write("Simulation time [s]");
							writeDelimiter();
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
				getBuffer().write(Integer.toString(simulationTime));
				writeDelimiter();
				for (float s : sensors) {
					if (Float.isNaN(s)) {
						break;
					}
					getBuffer().write(Float.toString(s));
					writeDelimiter();
				}
				getBuffer().newLine();
			} catch (IOException e) {
				flushAndClose();
				throw new PluginControllerException(getName() + ": Error writing data to file.", e);
			}
		}
		return null;
	}

	@Override
	public void terminateSimulation(Map<String, Object> parameters) {
		flushAndClose();
	}
	
	/**
	 * Writes a delimiter to the CSV.
	 * @throws IOException
	 */
	private void writeDelimiter() throws IOException {
		mBuffer.write(getProp(DELIMITER_KEY).getString());
	}

	/**
	 * Flushes the buffer and closes the file.
	 */
	private void flushAndClose() {
		try {
			getBuffer().flush();
			getBuffer().close();
			getWriter().close();
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
