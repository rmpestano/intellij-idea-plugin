/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.plugin.idea.service.threads;

import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.plugin.idea.util.CommandUtil;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Asynchronously loads command list. <p/>
 *
 * Use {@link org.jboss.forge.plugin.idea.service.PluginService} to access command cache.
 *
 * @author Adam Wyłuda
 */
public class CommandLoadingThread extends Thread
{
    private CountDownLatch latch = new CountDownLatch(1);
    private volatile List<UICommand> commands;
    private volatile UIContext uiContext;

    public CommandLoadingThread()
    {
        super("ForgeCommandLoadingThread");
    }

    /**
     * Invalidates stored command list.
     */
    public void invalidate()
    {
        commands = null;
    }

    /**
     * Makes (asynchronous) request to refresh a list of commands.
     */
    public void reload(UIContext uiContext)
    {
        this.uiContext = uiContext;
        latch.countDown();
    }

    /**
     * Returns stored commands, if they are not yet available, it loads them synchronously.
     */
    public List<UICommand> getCommands(UIContext uiContext)
    {
        if (commands == null)
        {
            commands = CommandUtil.getEnabledCommands(uiContext);
        }

        return commands;
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                // Wait for reload() call
                latch.await();
                commands = CommandUtil.getEnabledCommands(uiContext);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

}