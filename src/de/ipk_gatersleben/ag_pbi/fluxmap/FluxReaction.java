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

import java.util.ArrayList;
import java.util.HashMap;

import org.AttributeHelper;
import org.ErrorMsg;
import org.StringManipulationTools;
import org.graffiti.graph.Edge;
import org.graffiti.graph.Node;

public class FluxReaction {
	
	private ArrayList<FluxReactant> left, right;
	private boolean isCorrect = true;
	private final String reaction;
	private boolean reversible = false;
	
	public FluxReaction(String reaction) {
		
		this.reaction = reaction;
		left = new ArrayList<FluxReactant>();
		right = new ArrayList<FluxReactant>();
		try {
			
			boolean reactioncharFound = false;
			
			for (String reactionChar : new String[] { "<==>", "==>", "<==" }) {
				String[] leftright = splitReaction(reaction, reactionChar);
				if (leftright == null)
					continue;
				if (reactionChar.equals("<==>") || reactionChar.equals("==>")) {
					if (reactionChar.equals("<==>"))
						reversible = true;
					left = getReactants(leftright[0]);
					right = getReactants(leftright[1]);
					reactioncharFound = true;
				} else
					if (reactionChar.equals("<==")) {
						left = getReactants(leftright[1]);
						right = getReactants(leftright[0]);
						reactioncharFound = true;
					}
				break;
			}
			for (FluxReactant frl : left)
				for (FluxReactant frr : right)
					if (frl.getName().equals(frr.getName())) {
						ErrorMsg.addErrorMessage("Substance \"" + frl.getName() + "\" is educt and product of reaction<br>" +
								"\"" + reaction + "\"! Please use different substance names<br>(e.g. \"" + frl.getName() + "1\" and \"" + frl.getName() + "2\").");
						isCorrect = false;
					}
			if (!reactioncharFound) {
				ErrorMsg.addErrorMessage("Did not found any reaction arrow (<==>,==>,<==) in reaction \"" + reaction + "\"!");
				isCorrect = false;
			}
		} catch (Exception e) {
			ErrorMsg.addErrorMessage("<html>Exception occured when parsing reaction \"" + reaction + "\"!<br>" + e.getLocalizedMessage());
			isCorrect = false;
		}
	}
	
	private ArrayList<FluxReactant> getReactants(String reactionPart) {
		HashMap<String, FluxReactant> list = new HashMap<String, FluxReactant>();
		
		// TODO: how do we treat "A+ B"? because this will be parsed into one reaction...
		// maybe check for "+ " and " +" strings?
		for (String reactant : StringManipulationTools.splitSafe(reactionPart, " + ")) {
			FluxReactant fr = new FluxReactant(reactant);
			if (fr.isCorrect()) {
				// if there are reactants with the same name then add stocheometry
				FluxReactant existing = list.get(fr.getName());
				if (existing == null)
					list.put(fr.getName(), fr);
				else
					existing.add(fr);
			} else
				isCorrect = false;
		}
		
		return new ArrayList<FluxReactant>(list.values());
	}
	
	private String[] splitReaction(String reaction, String reactionChar) {
		if (reaction.contains(reactionChar)) {
			String left = reaction.substring(0, reaction.indexOf(reactionChar));
			String right = reaction.substring(reaction.indexOf(reactionChar) + reactionChar.length(), reaction.length());
			return new String[] { left, right };
		}
		return null;
	}
	
	public ArrayList<FluxReactant> getLeftReactants() {
		return left;
	}
	
	public ArrayList<FluxReactant> getRightReactants() {
		return right;
	}
	
	public ArrayList<FluxReactant> getAllReactants() {
		ArrayList<FluxReactant> list = new ArrayList<FluxReactant>();
		list.addAll(left);
		list.addAll(right);
		return list;
	}
	
	public boolean isLeftReactant(FluxReactant fr) {
		return left.contains(fr);
	}
	
	public boolean isCorrect() {
		return isCorrect;
	}
	
	public String getReaction() {
		return reaction;
	}
	
	public boolean isReversible() {
		return reversible;
	}
	
	public String getInfo(String reactionname) {
		return reactionname + "|||" + reaction;
	}
	
	public static String getInfoFromString(String infoString) {
		
		return null;
	}
	
	public static String getReactionNameFromInfo(String infoString) {
		if (infoString != null && infoString.contains("|||"))
			return StringManipulationTools.splitSafe(infoString, "|||")[0];
		else
			return "";
	}
	
	public static String getReactionnameFromInfo(String infoString) {
		if (infoString != null && infoString.contains("|||"))
			return StringManipulationTools.splitSafe(infoString, "|||")[1];
		else
			return null;
	}
	
	public void turnAround() {
		ArrayList<FluxReactant> temp = left;
		left = right;
		right = temp;
		if (!reversible)
			ErrorMsg.addErrorMessage("<html>Reaction \"" + reaction
					+ "\" has a negative flux value, but is irreversible. Continuing with turning around the reaction. Please check the template!");
	}
	
	public static void correctEdgeDirection(Edge ed, String info, boolean negativeFlux) {
		String reaction = getReactionnameFromInfo(info);
		if (reaction == null)
			return;
		
		FluxReaction fr = new FluxReaction(reaction);
		
		ArrayList<String> srclbls = AttributeHelper.getLabels(ed.getSource(), true);
		ArrayList<String> tgtlbls = AttributeHelper.getLabels(ed.getTarget(), true);
		
		boolean turnaround = false;
		lbls: for (String l : srclbls) {
			if (l == null || l.length() <= 0)
				continue;
			for (FluxReactant frt : fr.right)
				if (frt.getName().equalsIgnoreCase(l.trim())) {
					turnaround = true;
					break lbls;
				}
		}
		if (!turnaround)
			lbls: for (String l : tgtlbls) {
				if (l == null || l.length() <= 0)
					continue;
				for (FluxReactant frt : fr.left)
					// System.out.println("tgtlbls: " + frt.getName() + " " + l);
					if (frt.getName().equalsIgnoreCase(l.trim())) {
						turnaround = true;
						break lbls;
					}
			}
		
		if (negativeFlux)
			turnaround = !turnaround;
		
		if (turnaround) {
			Node src = ed.getSource();
			ed.setSource(ed.getTarget());
			ed.setTarget(src);
		}
	}
	
}