package de.ipk_gatersleben.ag_pbi.fluxmap;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import org.AttributeHelper;
import org.Vector2d;
import org.graffiti.editor.GravistoService;
import org.graffiti.editor.MainFrame;
import org.graffiti.editor.MessageType;
import org.graffiti.graph.Edge;
import org.graffiti.graph.Graph;
import org.graffiti.graph.GraphElement;
import org.graffiti.graph.Node;
import org.graffiti.plugin.algorithm.AbstractAlgorithm;
import org.graffiti.plugin.algorithm.ProvidesNodeContextMenu;
import org.graffiti.selection.Selection;

import de.ipk_gatersleben.ag_nw.graffiti.GraphHelper;
import de.ipk_gatersleben.ag_nw.graffiti.NodeTools;

public class ConnectMetabolites extends AbstractAlgorithm implements ProvidesNodeContextMenu {
	
	private static final Color linkColor = new Color(20, 100, 20);
	private final ImageIcon icon = new ImageIcon(GravistoService.getScaledImage(FluxMapAddon.getFluxIcon().getImage(), 16, 16));
	
	@Override
	public void execute() {
		throw new RuntimeException("Execute-method should not be called!");
	}
	
	@Override
	public String getName() {
		return "Connect Nodes";
	}
	
	@Override
	public JMenuItem[] getCurrentNodeContextMenuItem(Collection<Node> selectedNodes) {
		if (selectedNodes == null || selectedNodes.size() <= 0)
			return new JMenuItem[] { removeConnectedMetabolites() };
		else
			return new JMenuItem[] { getConnectMetabolites(selectedNodes), removeConnectedMetabolites() };
	}
	
	private JMenuItem removeConnectedMetabolites() {
		JMenuItem item = new JMenuItem("Disconnect Nodes");
		item.setIcon(icon);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<GraphElement> connectors = new ArrayList<GraphElement>();
				Graph g = MainFrame.getInstance().getActiveEditorSession().getGraph();
				for (Node nd : g.getNodes())
					if (AttributeHelper.hasAttribute(nd, "connector"))
						connectors.add(nd);
				
				g.deleteAll(connectors);
				MainFrame.showMessage(connectors.size() + " node" + (connectors.size() == 1 ? "" : "s") + " disconnected", MessageType.INFO);
			}
		});
		return item;
	}
	
	private JMenuItem getConnectMetabolites(final Collection<Node> selectedNodes) {
		JMenuItem item = new JMenuItem("Connect " + selectedNodes.size() + " Node" + (selectedNodes.size() == 1 ? "" : "s"));
		item.setIcon(icon);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				HashMap<String, ArrayList<Node>> lbl2nds = new HashMap<String, ArrayList<Node>>();
				HashSet<String> alreadyConnected = new HashSet<String>();
				Graph g = selectedNodes.iterator().next().getGraph();
				for (Node nd : g.getNodes()) {
					String lbl = AttributeHelper.getLabel(nd, null);
					if (lbl != null && lbl.length() > 0) {
						if (!lbl2nds.containsKey(lbl))
							lbl2nds.put(lbl, new ArrayList<Node>());
						lbl2nds.get(lbl).add(nd);
						if (AttributeHelper.hasAttribute(nd, "connector"))
							alreadyConnected.add(lbl);
					}
				}
				
				for (String lbl : alreadyConnected)
					lbl2nds.remove(lbl);
				
				ArrayList<String> alreadyProcessedLabels = new ArrayList<String>();
				ArrayList<Node> connectors = new ArrayList<Node>();
				
				for (Node nd : selectedNodes) {
					String lbl = AttributeHelper.getLabel(nd, null);
					if (lbl == null || lbl.length() <= 0 || alreadyProcessedLabels.contains(lbl) || !lbl2nds.containsKey(lbl))
						continue;
					
					Vector2d v = NodeTools.getCenter(lbl2nds.get(lbl));
					if (lbl2nds.get(lbl).size() == 1) {
						v.x += 50;
						v.y += 50;
					}
					
					Node connector = g.addNode(AttributeHelper.getDefaultGraphicsAttributeForNode(v));
					AttributeHelper.setOutlineColor(connector, linkColor);
					connectors.add(connector);
					AttributeHelper.setLabel(connector, lbl);
					AttributeHelper.setAttribute(connector, "", "connector", new Boolean(true));
					for (Node metab : lbl2nds.get(lbl)) {
						double out = 0, in = 0;
						for (Edge ed : metab.getAllOutEdges())
							if (isVisuallyInvertedEdge(ed))
								in += AttributeHelper.getFrameThickNess(ed);
							else
								out += AttributeHelper.getFrameThickNess(ed);
						for (Edge ed : metab.getAllInEdges())
							if (isVisuallyInvertedEdge(ed))
								out += AttributeHelper.getFrameThickNess(ed);
							else
								in += AttributeHelper.getFrameThickNess(ed);
						
						Edge newed = g.addEdge(in > out ? metab : connector, in > out ? connector : metab, true, AttributeHelper
									.getDefaultGraphicsAttributeForEdge(
											linkColor, linkColor, true));
						
						double val = Math.abs(in - out);
						if (val <= 1)
							AttributeHelper.setDashInfo(newed, 5, 5);
						AttributeHelper.setFrameThickNess(newed, val);
						AttributeHelper.setArrowSize(newed, val + 10);
					}
					alreadyProcessedLabels.add(lbl);
				}
				
				if (connectors.size() > 0) {
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().setActiveSelection(new Selection("connectors", connectors));
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().selectionChanged();
					
					GraphHelper.issueCompleteRedrawForActiveView();
				}
			}
		});
		return item;
	}
	
	public static boolean isVisuallyInvertedEdge(Edge ed) {
		String head = AttributeHelper.getArrowhead(ed);
		String tail = AttributeHelper.getArrowtail(ed);
		return (head == null || head.length() <= 0) && tail != null && tail.length() > 0;
	}
	
}
