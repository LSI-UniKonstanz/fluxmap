/*************************************************************************************
 * The Multimodal Datahandling Add-on is (c) 2008-2011 Plant Bioinformatics Group,
 * IPK Gatersleben, http://bioinformatics.ipk-gatersleben.de
 * The source code for this project, which is developed by our group, is
 * available under the GPL license v2.0 available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html. By using this
 * Add-on and VANTED you need to accept the terms and conditions of this
 * license, the below stated disclaimer of warranties and the licenses of
 * the used libraries. For further details see license.txt in the root
 * folder of this project.
 ************************************************************************************/
package de.ipk_gatersleben.ag_pbi.fluxmap;

import java.net.URL;

import javax.swing.ImageIcon;

import org.ErrorMsg;
import org.graffiti.attributes.StringAttribute;
import org.graffiti.editor.GravistoService;
import org.graffiti.plugin.algorithm.Algorithm;

import de.ipk_gatersleben.ag_nw.graffiti.plugins.addons.AddonAdapter;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.layout_control.dbe.TemplateFile;
import de.ipk_gatersleben.ag_pbi.datahandling.Template;

public class FluxMapAddon extends AddonAdapter {
	
	@Override
	@SuppressWarnings("unchecked")
	protected void initializeAddon() {
		
		// an attribute to make the flux reaction nodes nicer
		attributeComponents.put(FluxreactionAttribute.class, FluxreactionAttributeComponent.class);
		StringAttribute.putAttributeType(FluxreactionAttribute.name, FluxreactionAttribute.class);
		
		algorithms = new Algorithm[] {
				new VisualiseFluxDataAlgorithm(),
				new ConnectMetabolites(),
		};
		
		FluxExperimentDataLoader fl = new FluxExperimentDataLoader();
		Template fluxtempalte = new Template();
		URL url = GravistoService.getResource(this.getClass(), "flux_template", "xls");
		fluxtempalte.addTemplateFile(new TemplateFile("Flux Data", url, null));
		
		// does not work as intended
		// AttributeHelper.addEdgeShape("Dynamic Flux",
		// "de.ipk_gatersleben.ag_pbi.inputoutput.fluxdata.DynamicStraightLineEdgeShape");
		
		// this.views = new String[1];
		// this.views[0] =
		// "de.ipk_gatersleben.ag_pbi.inputoutput.fluxdata.FluxDataView";
		
		fluxtempalte.setTemplateLoader(fl);
		fl.registerLoader();
		fluxtempalte.registerTemplate();
		
		// AttributeHelper.setDeleteableAttribute("." + FluxreactionAttribute.path + "." + FluxreactionAttribute.name, FluxreactionAttribute.path);
		
	}
	
	@Override
	public ImageIcon getIcon() {
		return getFluxIcon();
	}
	
	public static ImageIcon getFluxIcon() {
		try {
			ImageIcon icon = new ImageIcon(GravistoService.getResource(FluxMapAddon.class, "fluxmap", "png"));
			return icon;
		} catch (Exception e) {
			ErrorMsg.addErrorMessage(e);
			return null;
		}
	}
}
