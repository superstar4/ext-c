/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
Copyright (c) 2010, Keith Cassell
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following 
      disclaimer in the documentation and/or other materials
      provided with the distribution.
    * Neither the name of the Victoria University of Wellington
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package nz.ac.vuw.ecs.kcassell.callgraph.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import nz.ac.vuw.ecs.kcassell.callgraph.CallGraphLink;
import nz.ac.vuw.ecs.kcassell.callgraph.CallGraphNode;
import nz.ac.vuw.ecs.kcassell.callgraph.JavaCallGraph;
import nz.ac.vuw.ecs.kcassell.cluster.ClustererIfc;
import nz.ac.vuw.ecs.kcassell.cluster.GraphBasedAgglomerativeClusterer;
import nz.ac.vuw.ecs.kcassell.cluster.MatrixBasedAgglomerativeClusterer;
import nz.ac.vuw.ecs.kcassell.cluster.MemberCluster;
import nz.ac.vuw.ecs.kcassell.similarity.DistanceCalculatorEnum;
import nz.ac.vuw.ecs.kcassell.similarity.DistanceCalculatorIfc;
import nz.ac.vuw.ecs.kcassell.similarity.IdentifierDistanceCalculator;
import nz.ac.vuw.ecs.kcassell.similarity.IdentifierGoogleDistanceCalculator;
import nz.ac.vuw.ecs.kcassell.similarity.IntraClassDistanceCalculator;
import nz.ac.vuw.ecs.kcassell.similarity.LevenshteinDistanceCalculator;
import nz.ac.vuw.ecs.kcassell.similarity.VectorSpaceModelCalculator;
import nz.ac.vuw.ecs.kcassell.utils.ApplicationParameters;
import nz.ac.vuw.ecs.kcassell.utils.EclipseUtils;
import nz.ac.vuw.ecs.kcassell.utils.ParameterConstants;

import org.eclipse.jdt.core.JavaModelException;

import edu.uci.ics.jung.visualization.VisualizationViewer;

public class ClusteringView implements ClusterUIConstants, ActionListener{

    /** The enclosing application of which this view is a part. */
	protected ExtC app = null;
	
	/** The identifier of the graph, usually the Eclipse handle. */
	protected String graphId = "";
        
	/** The main panel for this view. */
    protected JSplitPane mainPanel = null;
    
    /** Where descriptive text about the clusters is written. */
    protected JTextArea clustersTextArea = null;

	/** The visualization area for agglomerative clustering. */
    protected ClusteringGraphApplet clusteringApplet = null;

	public ClusteringView(ClusteringGraphApplet clusteringApplet, ExtC extC) {
		app = extC;
		this.clusteringApplet = clusteringApplet;
		clusteringApplet.setView(this);
		setUpView();
	}

	/**
	 * @return the mainPanel
	 */
	public JSplitPane getMainPanel() {
		return mainPanel;
	}
	
	/**
	 * @return the graphId
	 */
	public String getGraphId() {
		return graphId;
	}

	/**
	 * @return the visualization component containing the graph 
	 */
	protected VisualizationViewer<CallGraphNode, CallGraphLink> getVisualizer() {
		VisualizationViewer<CallGraphNode,CallGraphLink> visualizer =
			clusteringApplet.getVisualizer();
		return visualizer;
	}

	public ClusteringGraphApplet getClusteringApplet() {
		return clusteringApplet;
	}

	/**
	 * @return the clustersTextArea
	 */
	public JTextArea getClustersTextArea() {
		return clustersTextArea;
	}

	/**
	 * Creates the clustering applet and starts it
	 */
	protected void setUpView() {
		clustersTextArea = new JTextArea();
		clustersTextArea.setEditable(false);
		JScrollPane clusterScroller = new JScrollPane(clustersTextArea);
		clusteringApplet.setClustersTextArea(clustersTextArea);
		mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				clusterScroller, clusteringApplet);
        clusteringApplet.start();
		mainPanel.validate();
		mainPanel.repaint();
//		mainPanel.setDividerLocation(0.25);
	}

	/**
	 * Resets the agglomerative clusterer based on the chosen
	 * distance calculator.
	 */
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		String sourceName = "";
		if (source instanceof JComboBox) {
			JComboBox box = (JComboBox) source;
			sourceName = box.getName();
			if (AgglomerativeApplet.CALCULATOR_COMBO.equals(sourceName)) {
				handleCalculatorRequest(box);
			}
		}
	}

	protected void handleCalculatorRequest(JComboBox box) {
		Object selectedItem = box.getSelectedItem();
		String sCalculator = selectedItem.toString();
		ApplicationParameters parameters = ApplicationParameters.getSingleton();
		parameters.setParameter(ParameterConstants.CALCULATOR_KEY,
				sCalculator);
		JavaCallGraph callGraph = clusteringApplet.getGraph();
		if (callGraph == null) {
			callGraph = app.graphView.getGraph();
		}
		if (callGraph == null) {
			String msg = "Choose a class for agglomerative clustering.";
			JOptionPane.showMessageDialog(mainPanel, msg,
					"Choose Class", JOptionPane.INFORMATION_MESSAGE);
		} else {
			setUpAgglomerativeClustering(callGraph);
		}
	}

	/**
	 * Sets up the parts of the display that are calculator-dependent.
	 * @param callGraph
	 */
	public void setUpAgglomerativeClustering(JavaCallGraph callGraph) {
		AgglomerativeApplet aggApplet = (AgglomerativeApplet)clusteringApplet;
		ApplicationParameters parameters = ApplicationParameters.getSingleton();
		String sCalc =
			parameters.getParameter(ParameterConstants.CALCULATOR_KEY,
									DistanceCalculatorEnum.IntraClass.toString());

		try {
			if (DistanceCalculatorEnum.GoogleDistance.toString().equalsIgnoreCase(
					sCalc)) {
				IdentifierGoogleDistanceCalculator calc = new IdentifierGoogleDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else if (DistanceCalculatorEnum.Identifier.toString().equalsIgnoreCase(sCalc)) {
				IdentifierDistanceCalculator calc =
					new IdentifierDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else if (DistanceCalculatorEnum.IntraClass.toString().equalsIgnoreCase(
					sCalc)) {
				setUpIntraClassCalculation(callGraph);
			} else if (DistanceCalculatorEnum.Levenshtein.toString().equalsIgnoreCase(
					sCalc)) {
				LevenshteinDistanceCalculator calc = new LevenshteinDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else if (DistanceCalculatorEnum.VectorSpaceModel.toString().equalsIgnoreCase(
					sCalc)) {
				// TODO eliminate hard-coding
				String fileName =
		    	"c:/Tools/runtime-New_configuration/.metadata/.plugins/edu.wm.topicxp/CohesionTests/members";
				String classHandle =
					"=CohesionTests/src<nz.ac.vuw.ecs.kcassell.personcars{PersonCarDisjoint.java[PersonCarDisjoint";

				// If we're using the vector space model, we need to locate the
				// file containing the "member documents" for the class.
				if (DistanceCalculatorEnum.VectorSpaceModel.toString().equals(sCalc)) {
					JFileChooser chooser = new JFileChooser(ExtC.getLastDirAccessed());
					int option = chooser.showOpenDialog(app.frame);

					if (option == JFileChooser.APPROVE_OPTION) {
						File file = chooser.getSelectedFile();
						fileName = file.getAbsolutePath();
						ExtC.setLastDirAccessed(file.getParent());
					}
				}

				
				VectorSpaceModelCalculator calc = new VectorSpaceModelCalculator(fileName);
				List<String> names =
					EclipseUtils.getFilteredMemberHandles(classHandle);
				MatrixBasedAgglomerativeClusterer clusterer =
					new MatrixBasedAgglomerativeClusterer(names, calc);
				MemberCluster cluster = clusterer.getSingleCluster();
				String sCluster = cluster.toNestedString();
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else {
				String msg = "Unable to set up agglomerative clustering using " + sCalc;
				JOptionPane.showMessageDialog(mainPanel, msg,
						"Calculator Specification Error", JOptionPane.WARNING_MESSAGE);
			}
		} catch (Exception e) {
			String msg = "Unable to set up agglomerative clustering";
			JOptionPane.showMessageDialog(mainPanel, msg,
					"UI Error", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
		mainPanel.setDividerLocation(0.25);
	}

	/**
	 * Sets up the parts of the display that are calculator-dependent.
	 * @param callGraph
	 */
	public void setUpMixedModeClustering(JavaCallGraph callGraph) {
		graphId = callGraph.getGraphId();
		AgglomerativeApplet aggApplet = (AgglomerativeApplet)clusteringApplet;
		ApplicationParameters parameters = ApplicationParameters.getSingleton();
		String sCalc =
			parameters.getParameter(ParameterConstants.CALCULATOR_KEY,
									DistanceCalculatorEnum.IntraClass.toString());

		try {
			if (DistanceCalculatorEnum.Identifier.toString().equalsIgnoreCase(sCalc)) {
				IdentifierDistanceCalculator calc =
					new IdentifierDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else if (DistanceCalculatorEnum.IntraClass.toString().equalsIgnoreCase(
					sCalc)) {
				setUpIntraClassCalculation(callGraph);
			} else if (DistanceCalculatorEnum.GoogleDistance.toString().equalsIgnoreCase(
					sCalc)) {
				IdentifierGoogleDistanceCalculator calc = new IdentifierGoogleDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else if (DistanceCalculatorEnum.Levenshtein.toString().equalsIgnoreCase(
					sCalc)) {
				LevenshteinDistanceCalculator calc = new LevenshteinDistanceCalculator();
				String sCluster = clusterUsingIdentifiers(callGraph, calc);
				clustersTextArea.setText("Final cluster:\n" + sCluster);
				agglomerativePostProcessing(aggApplet);
			} else {
				String msg = "Unable to set up agglomerative clustering using " + sCalc;
				JOptionPane.showMessageDialog(mainPanel, msg,
						"Calculator Specification Error", JOptionPane.WARNING_MESSAGE);
			}
		} catch (Exception e) {
			String msg = "Unable to set up agglomerative clustering";
			JOptionPane.showMessageDialog(mainPanel, msg,
					"UI Error", JOptionPane.WARNING_MESSAGE);
			e.printStackTrace();
		}
		mainPanel.setDividerLocation(0.25);
	}

	/**
	 * Cluster based on the identifiers.
	 * @param callGraph
	 * @throws JavaModelException
	 */
	public static String clusterUsingIdentifiers(JavaCallGraph callGraph,
			DistanceCalculatorIfc<String> calc)
	throws JavaModelException {
		// TODO handles or simple names?
		List<String> names =
			EclipseUtils.getFilteredMemberNames(callGraph.getHandle());
//			EclipseUtils.getMemberHandles(callGraph.getHandle());
		MatrixBasedAgglomerativeClusterer clusterer =
			new MatrixBasedAgglomerativeClusterer(names, calc);
		MemberCluster cluster = clusterer.getSingleCluster();
		return cluster.toNestedString();
	}

	/**
	 * Sets up the parts of the view that are calculator dependent.
	 * @param callGraph
	 * @throws JavaModelException
	 */
	protected void setUpIntraClassCalculation(JavaCallGraph callGraph)
	throws IOException {
		if (callGraph != null) {
			JavaCallGraph undirectedGraph =
				JavaCallGraph.toUndirectedGraph(callGraph);
			IntraClassDistanceCalculator calculator =
				new IntraClassDistanceCalculator(undirectedGraph);
			GraphBasedAgglomerativeClusterer clusterer =
				new GraphBasedAgglomerativeClusterer(
					undirectedGraph, calculator);
			setUpClusteringApplet(clusterer, undirectedGraph);
		}
	}

	private void agglomerativePostProcessing(AgglomerativeApplet aggApplet) {
		String msg = "MatrixBasedAgglomerativeClusterer not fully implemented";
		JOptionPane.showMessageDialog(mainPanel, msg,
				"NYI", JOptionPane.WARNING_MESSAGE);
		JPanel south = aggApplet.setUpSouthPanel();
		aggApplet.getContentPane().add(south, BorderLayout.SOUTH);
	}

	public void setUpBetweennessClustering(JavaCallGraph callGraph) {
		if (callGraph != null) {
			graphId = callGraph.getGraphId();
			JavaCallGraph undirectedGraph =
				JavaCallGraph.toUndirectedGraph(callGraph);
			try {
				clusteringApplet.setUpView(undirectedGraph);
			} catch (IOException e) {
		        String msg = "Unable to set up betweenness clustering";
		        JOptionPane.showMessageDialog(mainPanel, msg, "UI Error",
		        		JOptionPane.WARNING_MESSAGE);
				e.printStackTrace();
			}
		}
		mainPanel.setDividerLocation(0.25);
	}

	/**
	 * Sets up the applet-specific parts of the view.
	 * @param clusterer
	 * @param callGraph
	 * @throws IOException
	 */
	protected void setUpClusteringApplet(ClustererIfc<CallGraphNode> clusterer,
			JavaCallGraph callGraph)
			throws IOException {
		if (callGraph != null) {
			clustersTextArea.setText("");
			clusteringApplet.setClusterer(clusterer);
			clusteringApplet.setUpView(callGraph);
		}
	}

}