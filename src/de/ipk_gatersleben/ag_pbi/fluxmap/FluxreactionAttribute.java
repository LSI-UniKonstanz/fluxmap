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

import org.AttributeHelper;
import org.graffiti.attributes.StringAttribute;
import org.graffiti.graph.Node;

public class FluxreactionAttribute extends StringAttribute {
	
	public static final String name = "fluxreactionsize";
	public static final String path = "flux";
	
	public FluxreactionAttribute() {
		super();
	}
	
	public FluxreactionAttribute(String id) {
		super(id);
	}
	
	public FluxreactionAttribute(String id, String value) {
		super(id, value);
	}
	
	public static void setNiceReaction(Node rnd, double size) {
		AttributeHelper.setAttribute(rnd, path, name, new FluxreactionAttribute(name, size + ""));
	}
	
	public static void removeNicereaction(Node rnd) {
		if (AttributeHelper.hasAttribute(rnd, path + SEPARATOR + name))
			AttributeHelper.deleteAttribute(rnd, path, name);
	}
	
}
