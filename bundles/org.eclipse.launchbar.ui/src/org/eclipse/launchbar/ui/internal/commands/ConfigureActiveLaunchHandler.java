/*******************************************************************************
 * Copyright (c) 2014 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Schaefer
 *******************************************************************************/
package org.eclipse.launchbar.ui.internal.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchMode;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPresentationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.launchbar.core.ILaunchDescriptor;
import org.eclipse.launchbar.core.internal.LaunchBarManager;
import org.eclipse.launchbar.ui.internal.Activator;
import org.eclipse.launchbar.ui.internal.dialogs.LaunchConfigurationEditDialog;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ConfigureActiveLaunchHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		LaunchBarManager launchBarManager = Activator.getDefault().getLaunchBarUIManager().getManager();
		ILaunchDescriptor launchDesc = launchBarManager.getActiveLaunchDescriptor();
		if (launchDesc == null)
			return Status.OK_STATUS;
		openConfigurationEditor(launchDesc);
		return Status.OK_STATUS;
	}

	
	public static IStatus canOpenConfigurationEditor(ILaunchDescriptor desc) {
		if (desc == null)
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Descriptor must not be null", new Exception("The launch descriptor must not be null."));
		LaunchBarManager manager = Activator.getDefault().getLaunchBarUIManager().getManager();
		ILaunchMode mode = manager.getActiveLaunchMode();
		IRemoteConnection target = manager.getActiveLaunchTarget();
		if (target == null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,  "No Active Target", new Exception("You must create a target to edit this launch configuration."));
		}
		
		ILaunchConfigurationType configType = null;
		try {
			configType = manager.getLaunchConfigurationType(desc, target);
		} catch(CoreException ce) {/* ignore */ };
		if (configType == null) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No launch configuration type", new Exception("Cannot edit this configuration"));
		}
		
		if( mode == null ) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No launch mode selected", new Exception("No launch mode selected"));
		}
		
		ILaunchGroup group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(configType, mode.getIdentifier());
		LaunchGroupExtension groupExt = null;
		if( group != null ) {
			groupExt = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(group.getIdentifier());
		}
		if (groupExt != null) {
			ILaunchConfiguration config = null;
			try {
				config = manager.getLaunchConfiguration(desc, target);
				if (config == null) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No launch configuration", new Exception("Cannot edit this configuration"));
				}
			} catch(CoreException ce) {
				
			}
			try {
				LaunchConfigurationPresentationManager mgr = LaunchConfigurationPresentationManager.getDefault();
				String mode2 = group.getMode();
				if( mode2 == null ) {
					return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No launch mode selected", new Exception("Cannot edit this configuration"));
				}
				ILaunchConfigurationTabGroup tabgroup = mgr.getTabGroup(config, mode.getIdentifier());
			} catch(CoreException ce) {
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,"No launch tabs defined.", new Exception("No launch tabs have been defined for this launch configuration type."));
			}
		} else {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,  "Cannot determine mode", new Exception("Cannot edit this configuration"));
		}
		return Status.OK_STATUS;
	}
	
	
	public static void openConfigurationEditor(ILaunchDescriptor desc) {
		if (desc == null)
			return;
		
		// Display the error message
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		IStatus s = canOpenConfigurationEditor(desc);
		if( !s.isOK()) {
			MessageDialog.openError(shell, s.getMessage(), s.getException() == null ? s.getMessage() : s.getException().getMessage());
		}
		
		// At this point, no error handling should be needed. 
		try {
			LaunchBarManager manager = Activator.getDefault().getLaunchBarUIManager().getManager();
			ILaunchMode mode = manager.getActiveLaunchMode();
			IRemoteConnection target = manager.getActiveLaunchTarget();
			ILaunchConfigurationType configType = manager.getLaunchConfigurationType(desc, target);
			ILaunchGroup group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(configType, mode.getIdentifier());
			LaunchGroupExtension groupExt = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(group.getIdentifier());
			ILaunchConfiguration config = manager.getLaunchConfiguration(desc, target);
			if (config.isWorkingCopy() && ((ILaunchConfigurationWorkingCopy) config).isDirty()) {
				config = ((ILaunchConfigurationWorkingCopy) config).doSave();
			}
			final LaunchConfigurationEditDialog dialog = new LaunchConfigurationEditDialog(shell, config, groupExt);
			dialog.setInitialStatus(Status.OK_STATUS);
			dialog.open();
		} catch (CoreException e2) {
			Activator.log(e2);
		}
	}
}
