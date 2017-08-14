package de.htw.berlin.io.csv.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.velasolaris.plugin.controller.spi.AbstractControllerPlugin;
import com.velasolaris.plugin.controller.spi.IPluginController;

/**
 * Controller plugin for Polysun. Adds the actors and sensors necessary for communicating with 4diac-RTE (FORTE).
 * @author Marc Jakobi</p>HTW Berlin</p>July 2017
 *
 */
public class IOControllerPlugin extends AbstractControllerPlugin {

	@Override
	public List<Class<? extends IPluginController>> getControllers(Map<String, Object> parameters) {
		List<Class<? extends IPluginController>> controllers = new ArrayList<>();
		controllers.add(CSVWriterController.class);
		controllers.add(MatWriterController.class);
		return controllers;
	}

	@Override
	public String getCreator() {
		return "Marc Jakobi, HTW Berlin";
	}

	@Override
	public String getDescription() {
		return "A Plugin for file input/output.";
	}
	
	@Override
	public String getVersion() {
		return "1.3.0";
	}
}
