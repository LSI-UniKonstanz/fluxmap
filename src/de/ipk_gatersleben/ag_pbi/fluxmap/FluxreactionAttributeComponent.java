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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.AttributeHelper;
import org.graffiti.attributes.Attributable;
import org.graffiti.attributes.Attribute;
import org.graffiti.graph.Edge;
import org.graffiti.graph.GraphElement;
import org.graffiti.graph.Node;
import org.graffiti.plugin.attributecomponent.AbstractAttributeComponent;
import org.graffiti.plugin.view.ShapeNotFoundException;

public class FluxreactionAttributeComponent extends AbstractAttributeComponent {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public void attributeChanged(Attribute attr) throws ShapeNotFoundException {
		recreate();
	}
	
	@Override
	public void recreate() throws ShapeNotFoundException {
		GraphElement ge = (GraphElement) this.attr.getAttributable();
		if (ge instanceof Node) {
			updatePosition(ge);
			adjustComponentSize();
			repaint();
		}
	}
	
	protected void updatePosition(Attributable attributable) {
		if (attributable instanceof Node) {
			Node n = (Node) attributable;
			Point2D pos = AttributeHelper.getPosition(n);
			double size = Double.parseDouble((String) attr.getValue()) / 2;
			setLocation((int) (pos.getX() - size - 1), (int) (pos.getY() - size - 1));
		}
	}
	
	@Override
	public void setShift(Point shift) {
		if (attr != null && attr.getAttributable() != null)
			updatePosition(attr.getAttributable());
	}
	
	@Override
	public void adjustComponentSize() {
		if (attr != null) {
			double size = Double.parseDouble((String) attr.getValue());
			setSize((int) size + 2, (int) size + 2);
		}
	}
	
	@Override
	public void paint(Graphics g) {
		super.paintComponent(g);
		Iterator<Edge> it = ((Node) attr.getAttributable()).getEdgesIterator();
		Color c = Color.black;
		if (it.hasNext())
			c = AttributeHelper.getFillColor(it.next());
		g.setColor(c);
		double size = Double.parseDouble((String) attr.getValue());
		g.fillOval(0, 0, (int) size, (int) size);
	}
	
}
