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

package nz.ac.vuw.ecs.kcassell;

import java.awt.Cursor;

import javax.swing.JFrame;

import nz.ac.vuw.ecs.kcassell.callgraph.JavaCallGraph;
import nz.ac.vuw.ecs.kcassell.callgraph.gui.ExtC;
import nz.ac.vuw.ecs.kcassell.utils.ApplicationParameters;
import nz.ac.vuw.ecs.kcassell.utils.EclipseUtils;
import nz.ac.vuw.ecs.kcassell.utils.ParameterConstants;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Pops up ExtC and shows the intra-class dependency graph for
 * the current Java file.
 * @see IWorkbenchWindowActionDelegate
 */
public class ShowGraphAction implements IWorkbenchWindowActionDelegate {
	// private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public ShowGraphAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		final ExtC app = ExtC.getSingleton();
		JFrame frame = app.getFrame();
		frame.toFront();
		
		try {
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			IType iType = EclipseUtils.getTypeInEditor();
			if (iType != null) {
				String handle = iType.getHandleIdentifier();
		    	ApplicationParameters params = app.getApplicationParameters();
		    	String sEdgeType = params.getParameter(
		    			ParameterConstants.EDGE_TYPE_KEY,
		    			EdgeType.DIRECTED.toString());
		    	EdgeType edgeType = EdgeType.valueOf(sEdgeType);
				JavaCallGraph callGraph = new JavaCallGraph(handle, edgeType);
				app.showGraph(callGraph);
			} else {
				// TODO notify user
			}
		} catch (Exception e) {
			//TODO notify user
			e.printStackTrace();
			//			app.loadProblemClassesFromDB();
		} finally {
			frame.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		// this.window = window;
	}
}