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

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.AttributeHelper;
import org.Colors;
import org.ErrorMsg;
import org.graffiti.editor.GravistoService;
import org.graffiti.editor.MainFrame;
import org.graffiti.graph.AdjListGraph;
import org.graffiti.graph.Edge;
import org.graffiti.graph.Graph;
import org.graffiti.graph.Node;
import org.graffiti.graphics.NodeLabelAttribute;
import org.graffiti.plugin.algorithm.AbstractEditorAlgorithm;
import org.graffiti.plugin.algorithm.Algorithm;
import org.graffiti.plugin.algorithm.PreconditionException;
import org.graffiti.plugin.parameter.Parameter;
import org.graffiti.plugin.view.View;
import org.graffiti.plugins.editcomponents.defaults.EdgeArrowShapeEditComponent;
import org.graffiti.selection.Selection;
import org.graffiti.session.EditorSession;

import de.ipk_gatersleben.ag_nw.graffiti.GraphHelper;
import de.ipk_gatersleben.ag_nw.graffiti.JLabelHTMLlink;
import de.ipk_gatersleben.ag_nw.graffiti.MyInputHelper;
import de.ipk_gatersleben.ag_nw.graffiti.NodeTools;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.editcomponents.chart_settings.GraffitiCharts;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.dbe.SplitNodeForSingleMappingData;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ConditionInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.EdgeHelper;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ExperimentInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.GraphElementHelper;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.NumericMeasurementInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SampleInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SubstanceInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.misc.svg_exporter.PngJpegAlgorithm;

public class VisualiseFluxDataAlgorithm extends AbstractEditorAlgorithm {
	private ConditionInterface selectedCondition;
	private SampleInterface selectedSample;
	private double globalMultiplicator = 1.0;
	private double headTailRatio = 1.5;
	private boolean showMeasurmentQuality = true;
	private Color lowestUncertainy = Color.BLACK;
	private Color highestUncertainy = Color.RED;
	private double minQuality, minQualityforvis;
	private double maxQuality, maxQualityforvis;
	private TreeSet<ConditionInterface> uniqueconditions;
	private ArrayList<SampleInterface> samples;
	public boolean removeEdgeCharts = new Boolean(true);
	public boolean removeEdgeBends = new Boolean(true);
	public ReactionNodeStyle style = ReactionNodeStyle.NORMALWITHLABEL;
	private double tolerancer = 0.001, tolerancem = 10;
	private boolean showFluxValues = false;
	private int fluxValuesDecimals = 3;
	
	@Override
	public boolean activeForView(View v) {
		return v != null;
	}
	
	@Override
	public void check() throws PreconditionException {
		if (graph == null)
			throw new PreconditionException("Graph is null!");
		
		boolean atleastOneEdgeHasMappingData = getPropertiesOfGraph(graph);
		
		if (!atleastOneEdgeHasMappingData)
			throw new PreconditionException("<html>Graph does not contain flux data. Please use the template<br>" +
																"from \"Experiments\" tab -> Data Input Templates");
		
		// PreconditionException e = new PreconditionException();
		//		
		// for (Node nd : getReactionNodes()) {
		// if (nd.getDegree() == 1)
		// continue;
		// if (nd.getOutDegree() <= 0)
		// e.add("Reaction " + AttributeHelper.getLabel(nd, "<error>") + " has no outgoing edge");
		// if (nd.getInDegree() <= 0)
		// e.add("Reaction " + AttributeHelper.getLabel(nd, "<error>") + " has no ingoing edge");
		// }
		// if (!e.isEmpty())
		// throw e;
		
	}
	
	private boolean getPropertiesOfGraph(Graph graph) {
		
		boolean atleastOneEdgeHasMappingData = false;
		
		maxQuality = Double.NEGATIVE_INFINITY;
		minQuality = Double.MAX_VALUE;
		HashSet<ConditionInterface> seriesIDCount = new HashSet<ConditionInterface>();
		TreeSet<SampleInterface> samplessorted = new TreeSet<SampleInterface>();
		
		for (Edge edge : graph.getEdges()) {
			
			ExperimentInterface exp = getDataMappings(edge);
			if (exp != null)
				for (SubstanceInterface s : exp)
					for (ConditionInterface c : s) {
						for (SampleInterface sa : c) {
							samplessorted.add(sa);
							if (!sa.iterator().hasNext())
								continue;
							NumericMeasurementInterface m = sa.iterator().next();
							
							if (isSubstanceNameIndicatingAFlux(m))
								atleastOneEdgeHasMappingData = true;
							
							if (m.getQualityAnnotation() != null) {
								try {
									double val = Double.valueOf(m.getQualityAnnotation());
									if (val > maxQuality)
										maxQuality = val;
									if (val < minQuality)
										minQuality = val;
								} catch (Exception e) {
									ErrorMsg.addErrorMessage("Could not parse quality annotation of edge " + edge);
								}
							}
						}
						seriesIDCount.add(c);
					}
		}
		
		minQualityforvis = minQuality;
		maxQualityforvis = maxQuality;
		
		samples = new ArrayList<SampleInterface>(samplessorted);
		uniqueconditions = new TreeSet<ConditionInterface>(seriesIDCount);
		
		if (uniqueconditions.size() <= 0 || samples.size() <= 0)
			return false;
		selectedCondition = uniqueconditions.iterator().next();
		selectedSample = samples.get(0);
		
		return atleastOneEdgeHasMappingData;
	}
	
	public static boolean isSubstanceNameIndicatingAFlux(NumericMeasurementInterface m) {
		return m.getParentSample().getParentCondition().getParentSubstance().getName().contains("^");
	}
	
	@Override
	public String getName() {
		return "Flux Visualisation";
	}
	
	@Override
	public String getCategory() {
		return "Mapping";
	}
	
	@Override
	public void execute() {
		MyInputHelper.getInput("[nonmodal,Close]", "Flux Visualisation Options", new Object[] { null, createPanel() });
		redraw(true);
	}
	
	@SuppressWarnings("unchecked")
	private JPanel createPanel() {
		int leftSize = 120;
		
		JPanel fluxPanel = new JPanel();
		fluxPanel.setLayout(new BoxLayout(fluxPanel, BoxLayout.Y_AXIS));
		
		final JSpinner mulSpinner = new JSpinner(new SpinnerNumberModel(globalMultiplicator, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1d));
		mulSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				globalMultiplicator = (Double) mulSpinner.getValue();
				redraw(false);
			}
		});
		fluxPanel.add(TableLayout.getSplit(new JLabel("Multiplicator"), mulSpinner, leftSize, TableLayout.FILL));
		
		final JSpinner arrowSpinner = new JSpinner(new SpinnerNumberModel(headTailRatio, 0d, 10d, 0.1d));
		arrowSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				headTailRatio = (Double) arrowSpinner.getValue();
				redraw(false);
			}
		});
		fluxPanel.add(TableLayout.getSplit(new JLabel("Head/Tail ratio"), arrowSpinner, leftSize, TableLayout.FILL));
		
		final JCheckBox fluxValuesCheckbox = new JCheckBox("Show Flux Values, with decimals:", showFluxValues);
		fluxValuesCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFluxValues = fluxValuesCheckbox.isSelected();
				redraw(true);
			}
		});
		final JSpinner fluxValuesSpinner = new JSpinner(new SpinnerNumberModel(fluxValuesDecimals,1,20,1));
		fluxValuesSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				fluxValuesDecimals = (Integer)fluxValuesSpinner.getValue();
				if(!fluxValuesCheckbox.isSelected())
					return ;
				
				redraw(true);
			}
		});
		fluxPanel.add(TableLayout.get3Split(new JLabel(), fluxValuesCheckbox, fluxValuesSpinner, leftSize, TableLayout.PREFERRED, TableLayout.FILL));
		fluxPanel.add(new JPanel());
		
		boolean qualityFound = minQuality != Double.NEGATIVE_INFINITY && minQuality != Double.MAX_VALUE;
		
		final JCheckBox activateQualityColor = new JCheckBox("Show", showMeasurmentQuality);
		activateQualityColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showMeasurmentQuality = activateQualityColor.isSelected();
				redraw(false);
			}
		});
		JButton colorChooserBtn = new JButton("Colors");
		colorChooserBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object[] res = MyInputHelper.getInput(
							"<html>Please specify the colors for the<br>" +
									"respective quality values.<br>&nbsp;",
							"Choose Quality Colors",
							new Object[] {
										"lowest quality value", minQuality,
										"low quality color", lowestUncertainy,
										"highest quality value", maxQuality,
										"high quality color", highestUncertainy,
					});
				
				if (res != null) {
					int i = 0;
					minQualityforvis = (Double) res[i++];
					lowestUncertainy = (Color) res[i++];
					maxQualityforvis = (Double) res[i++];
					highestUncertainy = (Color) res[i++];
				}
				if (showMeasurmentQuality)
					redraw(false);
			}
		});
		
		activateQualityColor.setEnabled(qualityFound);
		colorChooserBtn.setEnabled(qualityFound);
		fluxPanel.add(TableLayout.get3Split(new JLabel("Quality options"), activateQualityColor, colorChooserBtn, leftSize, TableLayout.PREFERRED,
					TableLayout.FILL));
		
		final JButton validater = new JButton("Validate Reactions");
		validater.setToolTipText("<html>Checks for all reactions, if the sum of ingoing fluxes equals the sum of<br>" +
												"outgoing fluxes. If this is not the case for a reaction, this might indicate<br>" +
												" an error in the template, e.g. substance weights missing, typing errors...)");
		validater.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<Node> unbrnds = new ArrayList<Node>();
				Collection<Node> rnds = getReactionNodes();
				
				for (Node nd : getGraph().getNodes()) {
					if (!rnds.contains(nd))
						continue;
					double in = 0d, out = 0d;
					for (Edge ed : nd.getEdges()) {
						// ignore all edges with dashed lines, because the fluxes are 0 then
						if (AttributeHelper.getDashInfo(ed) != null)
							continue;
						boolean directsaway = ed.getSource() == nd;
						if (directsaway)
							out += getFluxValue(ed);
						else
							in += getFluxValue(ed);
					}
					if (Math.abs(in - out) > tolerancer) {
						MainFrame.showMessageDialog("" +
								"<html>Found and selected unbalanced reaction \""
								+ AttributeHelper.getLabel(nd, "error") + "\"<br><br>" +
								"Ingoing mass:&nbsp; &nbsp;" + in + "<br>outgoing mass: " + out, "Unbalanced Reaction(s) Found");
						unbrnds.add(nd);
					}
				}
				if (unbrnds.size() > 0) {
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().setActiveSelection(new Selection("unbalanced reactions", unbrnds));
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().selectionChanged();
				} else
					MainFrame.showMessageDialog("All " + rnds.size() + " reactions are balanced", "Validation Successful");
			}
			
		});
		final JSpinner toleranceJSr = new JSpinner(new SpinnerNumberModel(tolerancer, 0d, Double.POSITIVE_INFINITY, 0.1d));
		toleranceJSr.setToolTipText("<html>The difference between in- and outgoing fluxes will be reported as unbalanced,<br>" +
													"if it exceeds the entered error acceptance trheshold.");
		toleranceJSr.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				tolerancer = (Double) toleranceJSr.getValue();
			}
		});
		final JButton validatem = new JButton("Validate Metabolites");
		validatem.setToolTipText("<html>Checks for all metabolites, if the sum of ingoing fluxes equals the sum of<br>" +
												"outgoing fluxes. If this is not the case, this might indicate an error in the<br>" +
												"template, e.g. missing reactions, missing substance weights, typing errors...)");
		validatem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<Node> unbrnds = new ArrayList<Node>();
				Collection<Node> rnds = getReactionNodes();
				
				for (Node nd : getGraph().getNodes()) {
					if (rnds.contains(nd))
						continue;
					double in = 0d, out = 0d;
					for (Edge ed : nd.getEdges()) {
						// ignore all edges with dashed lines, because the fluxes are 0 then
						if (AttributeHelper.getDashInfo(ed) != null)
							continue;
						boolean directsaway = ed.getSource() == nd;
						if (directsaway)
							out += getFluxValue(ed);
						else
							in += getFluxValue(ed);
					}
					if (Math.abs(in - out) > tolerancem) {
						MainFrame.showMessageDialog("" +
								"<html>Found and selected unbalanced metabolite \""
								+ AttributeHelper.getLabel(nd, "error") + "\"<br><br>" +
								"Ingoing mass:&nbsp; &nbsp;" + in + "<br>outgoing mass: " + out, "Unbalanced Reaction(s) Found");
						unbrnds.add(nd);
					}
				}
				if (unbrnds.size() > 0) {
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().setActiveSelection(new Selection("unbalanced metabolites", unbrnds));
					MainFrame.getInstance().getActiveEditorSession().getSelectionModel().selectionChanged();
				} else
					MainFrame.showMessageDialog("All " + rnds.size() + " metabolites are balanced", "Validation Successful");
			}
			
		});
		final JSpinner toleranceJSm = new JSpinner(new SpinnerNumberModel(tolerancem, 0d, Double.POSITIVE_INFINITY, 0.1d));
		toleranceJSm.setToolTipText("<html>The difference between in- and outgoing fluxes will be reported as unbalanced,<br>" +
													"if it exceeds the entered error acceptance trheshold.");
		toleranceJSm.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				tolerancem = (Double) toleranceJSm.getValue();
			}
		});
		
		fluxPanel.add(new JPanel());
		fluxPanel.add(TableLayout.getSplit(new JLabel("Flux Balance"),
				TableLayout.getSplitVertical(
						TableLayout.getSplit(validater, toleranceJSr, TableLayout.FILL, 60),
						TableLayout.getSplit(validatem, toleranceJSm, TableLayout.FILL, 60),
							TableLayout.PREFERRED, TableLayout.PREFERRED),
				leftSize, TableLayout.FILL));
		fluxPanel.add(new JPanel());
		
		final JButton makeSnapshot = new JButton("Create Snapshot");
		makeSnapshot.setToolTipText("<html>Creates a PNG-snapshot of the actual graph");
		makeSnapshot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GravistoService.getInstance().runAlgorithm(new PngJpegAlgorithm(false), getGraph(), new Selection(), null);
			}
		});
		fluxPanel.add(TableLayout.getSplit(null, makeSnapshot, 0, TableLayout.FILL));
		
		fluxPanel.add(new JPanel());
		
		JPanel optionPanel = new JPanel();
		optionPanel.setBorder(BorderFactory.createTitledBorder("Graph options"));
		optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
		
		final JButton splitSelectedNodes = new JButton("Split frequent nodes");
		splitSelectedNodes.setToolTipText("<html>By default a substance is represented by exactly one node.<br>" +
																"If your model contains a common substance (such as ATP or CO2),<br>" +
																"many edges will be connected to the node and thereby cluttering<br>" +
																"the graph. This command will create new node copies for each edge");
		splitSelectedNodes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				Collection<Node> substnds = new ArrayList<Node>(getGraph().getNodes());
				substnds.removeAll(getReactionNodes());
				
				ArrayList<NodeIntegerTuple> node2edgeCnt = new ArrayList<NodeIntegerTuple>();
				for (Node nd : substnds)
					if (nd.getDegree() > 1)
						node2edgeCnt.add(new NodeIntegerTuple(nd, nd.getDegree()));
				
				Collections.sort(node2edgeCnt, new Comparator<NodeIntegerTuple>() {
					@Override
					public int compare(NodeIntegerTuple o1, NodeIntegerTuple o2) {
						return o2.degree - o1.degree;
					}
				});
				
				if (node2edgeCnt.size() > 9)
					node2edgeCnt.subList(10, node2edgeCnt.size() - 1).clear();
				
				if (node2edgeCnt.size() <= 0) {
					MainFrame.showMessageDialog("No substance found, which is part of more than one reaction.", "Can't Split Node");
					return;
				}
				Object[] param = new Object[node2edgeCnt.size() * 2];
				int cnt = 0;
				for (NodeIntegerTuple nd : node2edgeCnt) {
					param[cnt++] = nd.degree + " times: " + AttributeHelper.getLabel(nd.nd, "error");
					param[cnt++] = new Boolean(false);
				}
				
				Object[] res = MyInputHelper.getInput(
							"<html>A list of substances is given, which show a high degree<br>" +
									"of interconnection. These might be common substances such as<br>" +
									"ATP or CO2. By selecting a substance, the corresponding node will<br>" +
									"be split into many nodes, improving the layout of the graph.<br>&nbsp;",
							"Choose Nodes For Splitting",
							param);
				
				if (res != null && res.length > 0) {
					getGraph().getListenerManager().transactionStarted(splitSelectedNodes);
					try {
						for (int i = 0; i < res.length; i++)
							if ((Boolean) res[i])
								SplitNodeForSingleMappingData.splitNodes(node2edgeCnt.get(i).nd, 1, getGraph(), true, true);
					} finally {
						getGraph().getListenerManager().transactionFinished(splitSelectedNodes);
					}
				}
				
			}
		});
		// final JButton rotategraph = new JButton("Rotate Graph");
		// rotategraph.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// GravistoService.getInstance().runAlgorithm(new RotateAlgorithm(), getGraph(), new Selection(), null);
		// }
		// });
		final JButton prettifyEdges = new JButton("Prettify Graph");
		prettifyEdges.setToolTipText("<html>Graph may be prettified by:<br><ul>" +
				"<li>Removing charts mapped onto the edges (because fluxes will be represented as thickness)</li>" +
				"<li>All edge bends will be deleted (use \"Edge - Bends...\" menu to introduce bends)</li>" +
				"<li>Switch between the representation of reaction nodes: normal or circular</li>");
		prettifyEdges.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<ReactionNodeStyle> list = new ArrayList<ReactionNodeStyle>();
				list.add(style);
				
				for (ReactionNodeStyle s : ReactionNodeStyle.values())
					if (!s.equals(style))
						list.add(s);
				
				Object[] result = MyInputHelper.getInput(
						"Please choose what to prettify",
						"Specify Parameters",
						new Object[] {
								"Remove Edge Charts", removeEdgeCharts,
								"Remove Edge Bends", removeEdgeBends,
								"Reaction node style", list,
						}
						);
				if (result != null) {
					style = (ReactionNodeStyle) result[2];
					prettifyGraph(getGraph(), (Boolean) result[0], (Boolean) result[1], style);
					// circReactNodes will be treated in redraw()
					redraw(true);
				}
				
			}
		});
		
		optionPanel.add(TableLayout.getSplit(splitSelectedNodes, prettifyEdges, TableLayout.FILL, TableLayout.FILL));
		
		// final JButton decreasenodesistance = new JButton("Decrease node distance");
		// decreasenodesistance.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// ExpandReduceLayouterAlgorithm.doOperation(getSelectedNodes(), 1 / 1.1, 1 / 1.1, "Decrease Space");
		// }
		// });
		// final JButton increasenodesistance = new JButton("Increase node distance");
		// increasenodesistance.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// ExpandReduceLayouterAlgorithm.doOperation(getSelectedNodes(), 1.1, 1.1, "Increase Space");
		// }
		// });
		// optionPanel.add(TableLayout.getSplit(decreasenodesistance, increasenodesistance, TableLayout.FILL, TableLayout.FILL));
		
		final JButton selectReactionNodes = new JButton("Select reaction nodes");
		selectReactionNodes.setToolTipText("<html>All reaction nodes will be selected. By using this selection,<br>" +
																"properties such as color and size may be altered.");
		selectReactionNodes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MainFrame.getInstance().getActiveEditorSession().getSelectionModel().setActiveSelection(new Selection("reactionnodes", getReactionNodes()));
				MainFrame.getInstance().getActiveEditorSession().getSelectionModel().selectionChanged();
			}
		});
		
		final JButton layout = new JButton("Layout Graph");
		layout.setToolTipText("<html>Layouts the graph with the DOT-Layout from http://www.graphviz.org/");
		layout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (MyEvilCopiedDotLayoutAlgorithm.isInstalled()) {
					GravistoService.getInstance().runAlgorithm(getDotLayouter(), getGraph(), new Selection(), null);
				} else {
					JPanel message = new JPanel();
					message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
					JLabel text = new JLabel("<html>" +
										"Could not detect DOT-Layout.<br>" +
										"Please use the \"Layout\" tab or download and install<br>" +
										"the GraphViz package from the following website:<br><br>");
					
					JLabelHTMLlink link = new JLabelHTMLlink("http://www.graphviz.org/", "http://www.graphviz.org/");
					message.add(text);
					message.add(link);
					JOptionPane.showMessageDialog(MainFrame.getInstance(), message, "DOT-Layout not found!", JOptionPane.ERROR_MESSAGE);
				}
				
			}
		});
		optionPanel.add(TableLayout.getSplit(selectReactionNodes, layout, TableLayout.FILL, TableLayout.FILL));
		
		fluxPanel.add(optionPanel);
		
		fluxPanel.add(new JPanel());
		
		if (uniqueconditions.size() > 1) {
			
			final JComboBox conditionBox = new JComboBox();
			for (ConditionInterface chosencondition : uniqueconditions)
				conditionBox.addItem(chosencondition);// , "<html>" + chosencondition.getSpecies() + "<br>" + chosencondition.getGenotype() + "<br>"
			// + chosencondition.getTreatment());
			
			conditionBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					selectedCondition = (ConditionInterface) conditionBox.getSelectedItem();
					redraw(false);
				}
			});
			fluxPanel.add(TableLayout.getSplit(new JLabel("Conditions"), conditionBox, leftSize, TableLayout.FILL));
			fluxPanel.add(new JPanel());
		}
		
		if (samples.size() > 1) {
			final JSlider jslider = new JSlider(0, samples.size() - 1, 0);
			Dictionary<Integer, JLabel> labels = jslider.getLabelTable();
			if (labels == null)
				labels = new Hashtable<Integer, JLabel>();
			int cnt = 0;
			for (SampleInterface sa : samples)
				labels.put(cnt++, new JLabel(sa.getSampleTime()));
			
			jslider.setLabelTable(labels);
			jslider.setMajorTickSpacing(1);
			jslider.setPaintTicks(true);
			jslider.setPaintLabels(true);
			jslider.setSnapToTicks(true);
			jslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent arg0) {
					selectedSample = samples.get(jslider.getValue());
					redraw(false);
				}
			});
			fluxPanel.add(TableLayout.getSplit(new JLabel("Timepoints"), jslider, leftSize, TableLayout.FILL));
			fluxPanel.add(new JPanel());
		}
		
		return fluxPanel;
	}
	
	protected void prettifyGraph(Graph g, boolean removeEdgeCharts, boolean removeEdgeBends, ReactionNodeStyle style) {
		this.removeEdgeCharts = removeEdgeCharts;
		this.removeEdgeBends = removeEdgeBends;
		this.style = style;
		if (removeEdgeCharts)
			for (Edge ed : g.getEdges())
				if (EdgeHelper.hasMappingData(ed))
					NodeTools.setNodeComponentType(ed, GraffitiCharts.HIDDEN.getName());
		if (removeEdgeBends)
			GraphHelper.removeBends(g, g.getEdges(), true);
		
	}
	
	protected Graph getGraph() {
		EditorSession es = MainFrame.getInstance().getActiveEditorSession();
		if (es != null && es.getGraph() != null)
			return es.getGraph();
		else {
			MainFrame.showMessageDialog("Please close flux visualisation dialog.", "All graphs closed");
			return new AdjListGraph();
		}
	}
	
	protected Algorithm getDotLayouter() {
		MyEvilCopiedDotLayoutAlgorithm alg = new MyEvilCopiedDotLayoutAlgorithm();
		Parameter[] params = alg.getParameters();
		params[3].setValue("Top-Down");
		return alg;
	}
	
	protected ArrayList<Node> getReactionNodes() {
		HashSet<String> reactionNames = new HashSet<String>();
		for (Edge edge : getGraph().getEdges()) {
			ExperimentInterface exp = getDataMappings(edge);
			if (exp != null)
				for (SubstanceInterface s : exp)
					reactionNames.add(FluxReaction.getReactionNameFromInfo(s.getInfo()));
		}
		ArrayList<Node> rnds = new ArrayList<Node>();
		for (Node nd : getGraph().getNodes()) {
			String lbl = AttributeHelper.getLabel(nd, null);
			if (lbl != null && reactionNames.contains(lbl))
				rnds.add(nd);
		}
		return rnds;
	}
	
	private void redraw(boolean forceredraw) {
		if (forceredraw || getGraph().getNumberOfNodes() + getGraph().getNumberOfNodes() > 200) {
			getGraph().getListenerManager().transactionStarted(this);
			try {
				doRedraw(getGraph());
			} finally {
				getGraph().getListenerManager().transactionFinished(this, true);
				GraphHelper.issueCompleteRedrawForGraph(getGraph());
			}
		} else
			doRedraw(getGraph());
	}
	
	public void doRedraw(Graph graph) {
		if (graph == null)
			graph = getGraph();
		graph.setModified(true);
		ArrayList<Node> nds = getReactionNodes();
		int power = (int) Math.pow(10,fluxValuesDecimals);
		
		String lbl = null;
		for (Edge ed : graph.getEdges()) {
			boolean edgechanged = false;
			ExperimentInterface exp = getDataMappings(ed);
			for (SubstanceInterface sub : exp)
				for (ConditionInterface con : sub)
					if (con.compareTo(selectedCondition) == 0) {
						for (SampleInterface sam : con)
							if (sam.compareTo(selectedSample) == 0) {
								if (!sam.iterator().hasNext())
									continue;
								
								try {
									NumericMeasurementInterface m = sam.iterator().next();
									
									double absoluteValue = Math.abs(m.getValue() * globalMultiplicator);

									lbl = Math.rint(((int) (Math.abs(m.getValue()) * power))) / power + "";
									AttributeHelper.setLabel(ed, lbl);
									AttributeHelper.setLabelColor(-1, ed, Color.red);
									AttributeHelper.getLabel(-1, ed).setFontSize(showFluxValues ? 12 : 0);
									
									FluxReaction.correctEdgeDirection(ed, sub.getInfo(), m.getValue() * globalMultiplicator < 0);
									
									// edge points to a reactionnode
									AttributeHelper.setArrowhead(ed, nds.contains(ed.getTarget()) ? "" : EdgeArrowShapeEditComponent.standardArrow);
									// set edge thickness
									if (absoluteValue < 0.01 && absoluteValue > -0.01) {// flux is nearly 0 -> gets dashed line
										AttributeHelper.setDashInfo(ed, 5, 10);
										AttributeHelper.setFrameThickNess(ed, 1);
										AttributeHelper.setArrowSize(ed, headTailRatio+3);
									} else {
										// all fluxes between 0 and 1 are set to 1
										if (absoluteValue < 0 && absoluteValue > -1)
											absoluteValue = -1;
										if (absoluteValue > 0 && absoluteValue < 1)
											absoluteValue = 1;
										AttributeHelper.setDashInfo(ed, null);
										AttributeHelper.setFrameThickNess(ed, absoluteValue);
										AttributeHelper.setArrowSize(ed, absoluteValue * headTailRatio+3);
//										AttributeHelper.setArrowhead(ed, EdgeArrowShapeEditComponent.standardArrow);
									}
									
									// adapt arrow head
									AttributeHelper.setArrowtail(ed, "");
									
									// colorize depending on quality value
									if (m.getQualityAnnotation() != null && showMeasurmentQuality) {
										Color color = Color.yellow;
										Float val = new Float((Double.parseDouble(m.getQualityAnnotation()) - minQualityforvis) / maxQualityforvis);
										if (!(val < 0 || val > 1))
											color = Colors.getColor(val,
													1d, lowestUncertainy, highestUncertainy);
										AttributeHelper.setFillColor(ed, color);
										AttributeHelper.setOutlineColor(ed, color);
									} else {
										AttributeHelper.setFillColor(ed, Color.BLACK);
										AttributeHelper.setOutlineColor(ed, Color.BLACK);
									}
									edgechanged = true;
								} catch (Exception e) {
									ErrorMsg.addErrorMessage(e);
								}
								break;
							}
					}
			if (!edgechanged) {// && exp != null && !exp.isEmpty()) {
				AttributeHelper.setDashInfo(ed, 5, 10);
				AttributeHelper.setFrameThickNess(ed, 1);
				AttributeHelper.setArrowSize(ed, headTailRatio+3);
				AttributeHelper.setFillColor(ed, Color.lightGray);
				AttributeHelper.setOutlineColor(ed, Color.lightGray);
			}
			
		}
		
		for (Node rnd : nds) {
			double size = 0;
			switch (style) {
				case DONT_CHANGE :
					break;
				case NOTHING:
					AttributeHelper.setSize(rnd, 0, 0);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, false);
					break;
				case NOTHINGWITHLABEL:
					AttributeHelper.setSize(rnd, 0, 0);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, true);
					break;
				case NICE:
					AttributeHelper.setSize(rnd, 0, 0);
					for (Edge ed : rnd.getEdges()) {
						double s = AttributeHelper.getFrameThickNess(ed);
						if (s > size)
							size = s;
					}
					FluxreactionAttribute.setNiceReaction(rnd, size);
					setReactionLabel(rnd, false);
					break;
				case NICEWITHLABEL:
					AttributeHelper.setSize(rnd, 0, 0);
					for (Edge ed : rnd.getEdges()) {
						double s = AttributeHelper.getFrameThickNess(ed);
						if (s > size)
							size = s;
					}
					FluxreactionAttribute.setNiceReaction(rnd, size);
					setReactionLabel(rnd, true);
					break;
				case SMALL:
					AttributeHelper.setSize(rnd, 5, 5);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, false);
					break;
				case SMALLWITHLABEL:
					AttributeHelper.setSize(rnd, 5, 5);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, true);
					break;
				case NORMAL:
					AttributeHelper.setSize(rnd, 25, 25);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, false);
					break;
				case NORMALWITHLABEL:
					AttributeHelper.setSize(rnd, 25, 25);
					FluxreactionAttribute.removeNicereaction(rnd);
					setReactionLabel(rnd, true);
					break;
				case ADDLABEL:
					setReactionLabel(rnd, true);
					break;
				case REMOVELABEL:
					setReactionLabel(rnd, false);
					break;
			}
		}
	}
	
	private void setReactionLabel(Node rnd, boolean set) {
		NodeLabelAttribute attr = AttributeHelper.getLabel(-1, rnd);
		if (attr != null) {
			String fs = attr.getFontStyle();
			if (!set && !fs.contains("mouseover"))
				attr.setFontStyle(fs + ",mouseover");
			if (set && fs.contains("mouseover"))
				attr.setFontStyle(fs.replace(",mouseover", ""));
		}
	}
	
	private ExperimentInterface getDataMappings(Edge edge) {
		return new GraphElementHelper(edge).getDataMappings();
	}
	
	public TreeSet<ConditionInterface> getUniqueconditions() {
		return uniqueconditions;
	}
	
	public void setSelectedCondition(ConditionInterface selectedCondition) {
		this.selectedCondition = selectedCondition;
	}
	
	public ConditionInterface getSelectedCondition() {
		return selectedCondition;
	}
	
	public static void prettifyGraphAfterMapping(Graph graph) {
		VisualiseFluxDataAlgorithm alg = new VisualiseFluxDataAlgorithm();
		if (!alg.getPropertiesOfGraph(graph))
			MainFrame.showMessageDialog("No flux data available", "Error");
		// alg.createPanel();
		alg.prettifyGraph(graph, true, true, ReactionNodeStyle.SMALL);
		alg.doRedraw(graph);
	}
	
	// private Collection<Node> getSelectedNodes() {
	// Collection<Node> nds = ((EditorSession) MainFrame.getInstance().getEditorSessionForGraph(getGraph())).getSelectionModel().getActiveSelection()
	// .getNodes();
	// if (nds.size() <= 0)
	// return getGraph.getNodes();
	// else
	// return nds;
	// }
	
	private class NodeIntegerTuple {
		
		private final Node nd;
		private final int degree;
		
		public NodeIntegerTuple(Node nd, int degree) {
			this.nd = nd;
			this.degree = degree;
		}
		
	}
	
	private double getFluxValue(Edge ed) {
		String lbl = AttributeHelper.getLabel(ed, "0");
		try {
			return Double.parseDouble(lbl);
		} catch (Exception err) {
			System.err.println(lbl);
			ErrorMsg.addErrorMessage("Cannot determine flux value, because edge label \"" + lbl + "\" is no number!");
			return 0;
		}
	}
	
}