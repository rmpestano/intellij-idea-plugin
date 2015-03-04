/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.plugin.idea.service;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.extensions.PluginId;

import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.furnace.util.AddonCompatibilityStrategies;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.Future;

/**
 * This is a singleton for the {@link Furnace} class.
 * <p/>
 * Use {@link org.jboss.forge.plugin.idea.service.ServiceHelper#loadFurnaceAndRun(Runnable)} to start any interaction
 * with Forge.
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author Adam Wyłuda
 */
@State(
         name = "ForgeConfiguration",
         storages = {
                  @Storage(id = "other", file = StoragePathMacros.APP_CONFIG + "/other.xml")
         })
public class ForgeService implements ApplicationComponent, PersistentStateComponent<ForgeService.State>
{
   private transient Furnace furnace;

   private State state = new State();

   ForgeService()
   {
   }

   public static ForgeService getInstance()
   {
      return ServiceManager.getService(ForgeService.class);
   }

   @Override
   public void initComponent()
   {
      createFurnace();
      initializeAddonRepositories(true);
      // Using a lenient addon compatibility strategy
      furnace.setAddonCompatibilityStrategy(AddonCompatibilityStrategies.LENIENT);
   }

   @Override
   public void disposeComponent()
   {
      stop();
   }

   @NotNull
   @Override
   public String getComponentName()
   {
      return getClass().getSimpleName();
   }

   Future<Furnace> startAsync()
   {
      return furnace.startAsync();
   }

   void stop()
   {
      furnace.stop();
   }

   public CommandFactory getCommandFactory()
   {
      return lookup(CommandFactory.class);
   }

   public CommandControllerFactory getCommandControllerFactory()
   {
      return lookup(CommandControllerFactory.class);
   }

   public ConverterFactory getConverterFactory()
   {
      return lookup(ConverterFactory.class);
   }

   public <S> S lookup(Class<S> service)
   {
      Imported<S> exportedInstance = null;
      if (furnace != null)
      {
         exportedInstance = furnace.getAddonRegistry().getServices(service);
      }
      return (exportedInstance == null || exportedInstance.isUnsatisfied()) ? null : exportedInstance.get();
   }

   @SuppressWarnings("unchecked")
   public <T> Class<T> locateNativeClass(Class<T> type)
   {
      Class<T> result = type;
      AddonRegistry registry = furnace.getAddonRegistry();
      for (Addon addon : registry.getAddons())
      {
         try
         {
            ClassLoader classLoader = addon.getClassLoader();
            result = (Class<T>) classLoader.loadClass(type.getName());
            break;
         }
         catch (ClassNotFoundException e)
         {
         }
      }
      return result;
   }

   public boolean isLoaded()
   {
      return furnace != null && furnace.getStatus().isStarted();
   }

   void createFurnace()
   {
      // MODULES-136
      System.setProperty("modules.ignore.jdk.factory", "true");

      furnace = FurnaceFactory.getInstance(ForgeService.class.getClassLoader());
   }

   void initializeAddonRepositories(boolean addBundledAddons)
   {
      furnace.addRepository(AddonRepositoryMode.MUTABLE, new File(state.addonDir));

      if (addBundledAddons)
      {
         PluginId pluginId = PluginManager.getPluginByClassName(getClass()
                  .getName());
         File pluginHome = new File(PathManager.getPluginsPath(),
                  pluginId.getIdString());
         File addonRepo = new File(pluginHome, "addon-repository");
         furnace.addRepository(AddonRepositoryMode.IMMUTABLE, addonRepo);
      }
   }

   @Nullable
   @Override
   public State getState()
   {
      return this.state;
   }

   @Override
   public void loadState(State state)
   {
      this.state = state;
   }

   public static class State
   {
      private String addonDir = new File(OperatingSystemUtils.getUserForgeDir(), "addons").getAbsolutePath();
      private boolean cacheCommands = true;

      public String getAddonDir()
      {
         return addonDir;
      }

      public void setAddonDir(String addonDir)
      {
         this.addonDir = addonDir;
      }

      public boolean isCacheCommands()
      {
         return cacheCommands;
      }

      public void setCacheCommands(boolean cacheCommands)
      {
         this.cacheCommands = cacheCommands;
      }
   }
}
