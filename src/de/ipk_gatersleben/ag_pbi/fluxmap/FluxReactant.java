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

import org.ErrorMsg;
import org.StringManipulationTools;

public class FluxReactant {
	
	private double coeff = 1d;
	private String name;
	private boolean isCorrect = true;
	
	public FluxReactant(String reactant) {
		reactant = FluxExperimentDataLoader.trimSpaceChars(reactant);
		if (isCoefficientSpecified(reactant)) {
			String coeffstr = getCoefficient(reactant);
			try {
				coeff = Double.parseDouble(coeffstr);
			} catch (Exception e) {
				try {
					coeffstr = StringManipulationTools.stringReplace(coeffstr, ",", ".");
					coeff = Double.parseDouble(coeffstr);
				} catch (Exception e2) {
					ErrorMsg.addErrorMessage("<html>Could not parse stoichiometric coefficient of \"" + reactant + "\"!<br>" +
							"Was \" + \" used to separate the reactants?");
					isCorrect = false;
				}
			}
			name = reactant.substring(coeffstr.length()).trim();
		} else {
			coeff = 1d;
			name = reactant.trim();
		}
	}
	
	private boolean isCoefficientSpecified(String reactant) {
		// String ".*[0-9]{1} [0-9]*[A-Z]+.*"
		// String startsWithNumberPattern = "[0-9]+.*";
		// String startsWithDotPattern = "\\.[0-9]+.*";
		return reactant.indexOf(" ") > 0;
	}
	
	private String getCoefficient(String reactant) {
		return reactant.substring(0, reactant.indexOf(" "));
	}
	
	public double getCoeff() {
		return coeff;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isCorrect() {
		return isCorrect;
	}
	
	/**
	 * add Fluxreactants with the same name only!
	 */
	public void add(FluxReactant otherReactant) {
		if (getName().equals(otherReactant.getName()))
			coeff += otherReactant.getCoeff();
		else
			ErrorMsg.addErrorMessage("Merging of two Fluxreactants with different names (" + getName() + "," + otherReactant.getName() + ") was ignored");
		
	}
	
}