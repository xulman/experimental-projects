/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.mastodon.mamut.experimental;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.KeyConfigScopes;
import org.mastodon.mamut.experimental.spots.RotateSpotsGeneral;
import org.mastodon.mamut.experimental.spots.RotateSpotsInPlane;
import org.mastodon.mamut.experimental.spots.ShiftSpots;
import org.mastodon.mamut.experimental.trees.LineageRandomColorizer;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.ProjectModel;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;

import org.scijava.AbstractContextual;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.RunnableAction;

import net.imagej.ImageJ;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.WindowManager;
import javax.swing.WindowConstants;

@Plugin( type = MamutPlugin.class )
public class ExperimentalPluginsFacade extends AbstractContextual implements MamutPlugin
{
	//"IDs" of all plug-ins wrapped in this class
	private static final String EXP_SHIFTSPOTS = "[vexp] shift spots";
	private static final String EXP_PLANEROTATESPOTS = "[vexp] in plane rotate spots";
	private static final String EXP_GENROTATESPOTS = "[vexp] general rotate spots";
	private static final String EXP_LINEAGECOLORIZER = "[vexp] random color tags";

	private static final String[] EXP_SHIFTSPOTS_KEYS = { "not mapped" };
	private static final String[] EXP_PLANEROTATESPOTS_KEYS = { "not mapped" };
	private static final String[] EXP_GENROTATESPOTS_KEYS = { "not mapped" };
	private static final String[] EXP_LINEAGECOLORIZER_KEYS = { "not mapped" };
	//------------------------------------------------------------------------

	/** titles of this plug-in's menu items */
	private static final Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put( EXP_SHIFTSPOTS, "Shift Spots" );
		menuTexts.put( EXP_PLANEROTATESPOTS, "Rotate Spots (In Plane)" );
		menuTexts.put( EXP_GENROTATESPOTS, "Rotate Spots (General)" );
		menuTexts.put( EXP_LINEAGECOLORIZER, "Random Color Lineages" );
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList(
			menu( "Plugins",
				menu( "Trees Management",
					item( EXP_LINEAGECOLORIZER )
				),
				item( EXP_SHIFTSPOTS ),
				item( EXP_PLANEROTATESPOTS ),
				item( EXP_GENROTATESPOTS )
			)
		);
	}

	/** Command descriptions for all provided commands */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.MAMUT, KeyConfigContexts.TRACKSCHEME, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(EXP_SHIFTSPOTS, EXP_SHIFTSPOTS_KEYS, "Scale and translate spots coordinates in both spatial and temporal domains.");
			descriptions.add(EXP_PLANEROTATESPOTS, EXP_PLANEROTATESPOTS_KEYS, "Rotate spots in spatial domain, in a simply-given plane.");
			descriptions.add(EXP_GENROTATESPOTS, EXP_GENROTATESPOTS_KEYS, "Rotate spots in spatial domain, in a very general way.");
			descriptions.add(EXP_LINEAGECOLORIZER, EXP_LINEAGECOLORIZER_KEYS, "Assign to every lineage tree a randomly chosen color from the selected tag set.");
		}
	}
	//------------------------------------------------------------------------


	private final AbstractNamedAction actionShiftSpots;
	private final AbstractNamedAction actionPlaneRotateSpots;
	private final AbstractNamedAction actionGenRotateSpots;
	private final AbstractNamedAction actionLineageColorizer;

	/** reference to the currently available project in Mastodon */
	private ProjectModel pluginAppModel;

	/** default c'tor: creates Actions available from this plug-in */
	public ExperimentalPluginsFacade()
	{
		actionShiftSpots = new RunnableAction(EXP_SHIFTSPOTS, this::shiftSpots);
		actionPlaneRotateSpots = new RunnableAction(EXP_PLANEROTATESPOTS, this::rotateSpotsInPlane);
		actionGenRotateSpots = new RunnableAction(EXP_GENROTATESPOTS, this::rotateSpotsGeneral);
		actionLineageColorizer = new RunnableAction(EXP_LINEAGECOLORIZER, this::lineageColorizer);
		updateEnabledActions();
	}

	/** register the actions to the application (with no shortcut keys) */
	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction(actionShiftSpots, EXP_SHIFTSPOTS_KEYS);
		actions.namedAction(actionPlaneRotateSpots, EXP_PLANEROTATESPOTS_KEYS);
		actions.namedAction(actionGenRotateSpots, EXP_GENROTATESPOTS_KEYS);
		actions.namedAction(actionLineageColorizer , EXP_LINEAGECOLORIZER_KEYS);
	}

	/** learn about the current project's params */
	@Override
	public void setAppPluginModel( final ProjectModel model )
	{
		//the application reports back to us if some project is available
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	/** enables/disables menu items based on the availability of some project */
	private void updateEnabledActions()
	{
		actionShiftSpots.setEnabled( pluginAppModel != null );
		actionPlaneRotateSpots.setEnabled( pluginAppModel != null );
		actionGenRotateSpots.setEnabled( pluginAppModel != null );
		actionLineageColorizer.setEnabled( pluginAppModel != null );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void shiftSpots()
	{
		this.getContext().getService(CommandService.class).run(
			ShiftSpots.class, true,
			"appModel", pluginAppModel
		);
	}

	private void rotateSpotsInPlane()
	{
		this.getContext().getService(CommandService.class).run(
			RotateSpotsInPlane.class, true,
			"appModel", pluginAppModel
		);
	}

	private void rotateSpotsGeneral()
	{
		this.getContext().getService(CommandService.class).run(
			RotateSpotsGeneral.class, true,
			"appModel", pluginAppModel
		);
	}

	private void lineageColorizer()
	{
		this.getContext().getService(CommandService.class).run(
			LineageRandomColorizer.class, true,
			"pluginAppModel", pluginAppModel
		);
	}
}
