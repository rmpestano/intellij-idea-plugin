/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.plugin.idea.service.callbacks;

import java.util.List;

import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextListener;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.plugin.idea.context.UIContextFactory;
import org.jboss.forge.plugin.idea.service.ForgeService;
import org.jboss.forge.plugin.idea.service.PluginService;
import org.jboss.forge.plugin.idea.ui.CommandListPopupBuilder;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author <a href="mailto:danielsoro@gmail.com">Daniel Cunha (soro)</a>
 */
public class CommandListPopupCallBack implements Runnable
{
    private final Project project;
    private final VirtualFile[] selectedFiles;

    public CommandListPopupCallBack(Project project, VirtualFile... selectedFiles)
    {
        this.project = project;
        this.selectedFiles = selectedFiles;

    }

    @Override
    public void run()
    {
        UIContext uiContext = UIContextFactory.create(project, selectedFiles);

        List<UICommand> candidates = PluginService.getInstance().getEnabledCommands(uiContext);

        new CommandListPopupBuilder()
                .setUIContext(uiContext)
                .setCommands(candidates)
                .setRecentCommands(PluginService.getInstance().getRecentCommands(candidates, uiContext))
                .build()
                .showCenteredInCurrentWindow(project);
    }

}
