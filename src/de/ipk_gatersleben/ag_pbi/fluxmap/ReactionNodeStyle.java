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

public enum ReactionNodeStyle {
	DONT_CHANGE("Do not change"),
	NOTHING("Invisible"), NOTHINGWITHLABEL("Invisible with label"), NICE("Rounded"), NICEWITHLABEL("Rounded with label"),
	SMALL("Small"), SMALLWITHLABEL("Small with label"), NORMAL("Normal"), NORMALWITHLABEL("Normal with label"),
	ADDLABEL("Add only label"), REMOVELABEL("Remove only label");
	
	private String description;
	
	ReactionNodeStyle(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
}
