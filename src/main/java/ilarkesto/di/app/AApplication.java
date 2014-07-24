/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>, Artjom Kochtchi
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package ilarkesto.di.app;

import ilarkesto.base.Str;
import ilarkesto.base.Sys;
import ilarkesto.base.Tm;
import ilarkesto.base.Utl;
import ilarkesto.concurrent.ATask;
import ilarkesto.concurrent.TaskManager;
import ilarkesto.core.logging.Log;
import ilarkesto.core.persistance.AEntityDatabase;
import ilarkesto.core.time.DateAndTime;
import ilarkesto.core.time.TimePeriod;
import ilarkesto.di.Context;
import ilarkesto.integration.xstream.XStreamSerializer;
import ilarkesto.io.ExclusiveFileLock;
import ilarkesto.io.ExclusiveFileLock.FileLockedException;
import ilarkesto.io.AFileStorage;
import ilarkesto.io.IO;
import ilarkesto.io.SimpleFileStorage;
import ilarkesto.io.Zip;
import ilarkesto.logging.DefaultLogRecordHandler;
import ilarkesto.persistence.DaoListener;
import ilarkesto.persistence.DaoService;
import ilarkesto.persistence.EntityStore;
import ilarkesto.persistence.FileEntityStore;
import ilarkesto.persistence.Serializer;
import ilarkesto.persistence.TransactionService;
import ilarkesto.properties.FilePropertiesStore;

import java.io.File;
import java.io.FileFilter;
import java.util.Set;

/**
 * Base class of a custom application
 * 
 * @author Witoslaw Koczewski
 */
public abstract class AApplication {

	protected Log log = Log.get(getClass());

	private ExclusiveFileLock exclusiveFileLock;
	private boolean startupFailed;
	private boolean shuttingDown;
	private boolean shutdown;

	private BuildProperties buildProperties;

	protected abstract void onStart();

	protected abstract void onShutdown();

	protected abstract void scheduleTasks(TaskManager tm);

	protected Context context;
	private String[] arguments = new String[0];

	public void ensureIntegrity() {
		log.info("Ensuring application integrity");

		getDaoService().ensureIntegrity();
		getTransactionService().commit();

		if (AEntityDatabase.instance != null) AEntityDatabase.instance.ensureIntegrityForAllEntities();
	}

	protected boolean isSingleton() {
		return true;
	}

	public final void start() {
		if (instance != null) { throw new RuntimeException("An Application already started: " + instance); }
		synchronized (getApplicationLock()) {

			instance = this;

			log.info("\n\n     DATA PATH:", getApplicationDataDir(), "\n\n");

			context = Context.createRootContext("app:" + getApplicationName());
			context.addBeanProvider(this);

			if (isSingleton()) {
				File lockFile = new File(getApplicationDataDir() + "/.lock");
				for (int i = 0; i < 10; i++) {
					try {
						exclusiveFileLock = new ExclusiveFileLock(lockFile);
						break;
					} catch (FileLockedException ex) {
						log.info("Application already running. Lock file locked: " + lockFile.getAbsolutePath());
					}
					Utl.sleep(1000);
				}
				if (exclusiveFileLock == null) {
					log.fatal("Application startup failed. Another instance is running. Lock file: "
							+ lockFile.getAbsolutePath());
					shutdown();
					return;
				}
			}

			try {
				getApplicationConfig();
			} catch (Throwable ex) {
				startupFailed = true;
				throw new RuntimeException("Application startup failed. Loading configuration failed.", ex);
			}
			try {
				backupApplicationDataDir();
			} catch (Throwable ex) {
				log.error("Backing up application data directory failed.", ex);
			}
			try {
				ensureIntegrity();
			} catch (Throwable ex) {
				startupFailed = true;
				shutdown(false);
				throw new RuntimeException("Application startup failed. Data integrity check or repair failed.", ex);
			}
			try {
				onStart();
			} catch (Throwable ex) {
				startupFailed = true;
				shutdown(false);
				throw new RuntimeException("Application startup failed.", ex);
			}

			try {
				scheduleTasks(getTaskManager());
			} catch (Throwable ex) {
				startupFailed = true;
				shutdown(true);
				throw new RuntimeException("Application startup failed.", ex);
			}
		}
	}

	public final void shutdown() {
		shutdown(true);
	}

	private final void shutdown(final boolean runOnShutdown) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				synchronized (getApplicationLock()) {
					if (instance == null) throw new RuntimeException("Application not started yet.");
					log.info("Shutdown initiated:", getApplicationName());

					if (runOnShutdown) onShutdown();

					getTaskManager().shutdown(10000);
					Set<ATask> tasks = getTaskManager().getRunningTasks();
					if (!tasks.isEmpty()) {
						log.warn("Aborting tasks on shutdown failed:", tasks);
					}
					getEntityStore().lock();
					shutdown = true;

					if (context != null) context.destroy();

					if (exclusiveFileLock != null) exclusiveFileLock.release();
					Log.flush();
					DefaultLogRecordHandler.stopLogging();
				}
			}

		});
		thread.setName(getApplicationName() + " shutdown");
		shuttingDown = true;
		thread.start();
	}

	public void backupApplicationDataDir() {
		final File dataDir = new File(getApplicationDataDir());
		File backupFile = new File(dataDir.getPath() + "/backups/" + getApplicationName() + "-data_"
				+ DateAndTime.now().formatLog() + ".zip");
		log.info("Backing up application data dir:", dataDir.getAbsolutePath(), "into", backupFile);
		long starttime = Tm.getCurrentTimeMillis();
		Object lock = entityStore == null ? this : entityStore;
		synchronized (lock) {
			Zip.zip(backupFile, new File[] { dataDir }, new FileFilter() {

				@Override
				public boolean accept(File file) {
					File dir = file.getParentFile();
					if (dir.equals(dataDir)) {
						// base dir
						String name = file.getName();
						if (name.equals(".lock")) return false;
						if (name.equals("backups")) return false;
						if (name.equals("entities-rescue")) return false;
						if (name.equals("tmp")) return false;
						if (name.startsWith("gwt-")) return false;
						if (file.isDirectory()) log.info("    Zipping", file.getPath());
					}
					return true;
				}
			});
		}
		long runtime = Tm.getCurrentTimeMillis() - starttime;
		log.info("  Backup completed in", new TimePeriod(runtime).toShortestString());
		deleteOldApplicationDataDirBackups();
	}

	private void deleteOldApplicationDataDirBackups() {
		File backupDir = new File(getApplicationDataDir() + "/backups");
		File[] files = backupDir.listFiles();
		if (files == null || files.length == 0) return;

		log.info("Deleting old backup files from", backupDir);
		final long deadline = Tm.getCurrentTimeMillis() - Tm.DAY * 7;

		for (File file : files) {
			if (!file.getName().startsWith(getApplicationName())) continue;
			if (file.lastModified() >= deadline && !file.getName().endsWith(".zip~")) continue;
			log.debug("    Deleting", file);
			IO.delete(file);
		}
	}

	public final <T> T autowire(T bean) {
		return context.autowire(bean);
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	public String[] getArguments() {
		return arguments;
	}

	private static AApplication instance;

	public static AApplication get() {
		if (instance == null) throw new RuntimeException("No application started yet");
		return instance;
	}

	public static boolean isStarted() {
		return instance != null;
	}

	private static String APPLICATION_LOCK = "APPLICATION_LOCK";

	public Object getApplicationLock() {
		return APPLICATION_LOCK;
	}

	public final String getApplicationPackageName() {
		return getClass().getPackage().getName();
	}

	public String getApplicationLabel() {
		return getApplicationName();
	}

	public AApplication getApplication() {
		return this;
	}

	private String applicationName;

	public String getApplicationName() {
		if (applicationName == null) {
			applicationName = getClass().getSimpleName();
			applicationName = Str.lowercaseFirstLetter(applicationName);
			applicationName = Str.removeSuffix(applicationName, "Application");
		}
		return applicationName;
	}

	private SimpleFileStorage fileStorage;

	public AFileStorage getFileStorage() {
		if (fileStorage == null) fileStorage = new SimpleFileStorage(new File(getApplicationDataDir()));
		return fileStorage;
	}

	private String applicationDataDir;

	public String getApplicationDataDir() {
		if (applicationDataDir == null) {
			if (isDevelopmentMode()) {
				String path = "runtimedata";
				applicationDataDir = new File(path).getAbsolutePath();
			} else {
				applicationDataDir = getProductionModeApplicationDataDir();
			}
		}
		return applicationDataDir;
	}

	protected String getProductionModeApplicationDataDir() {
		return Sys.getUsersHomePath() + "/." + getApplicationName();
	}

	private String applicationTempDir;

	public String getApplicationTempDir() {
		if (applicationTempDir == null) {
			applicationTempDir = getApplicationDataDir() + "/tmp";
		}
		return applicationTempDir;
	}

	private FilePropertiesStore applicationConfig;

	public FilePropertiesStore getApplicationConfig() {
		if (applicationConfig == null) {
			applicationConfig = new FilePropertiesStore(getApplicationDataDir() + "/config.properties", false);
		}
		return applicationConfig;
	}

	public BuildProperties getBuildProperties() {
		if (buildProperties == null) buildProperties = new BuildProperties(getClass());
		return buildProperties;
	}

	public boolean isDevelopmentMode() {
		return Sys.isDevelopmentMode();
	}

	public final boolean isProductionMode() {
		return !isDevelopmentMode();
	}

	public boolean isStartupFailed() {
		return startupFailed;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public final String toString() {
		return getApplicationName();
	}

	// --- beans / services ---

	private TaskManager taskManager;

	public TaskManager getTaskManager() {
		if (taskManager == null) taskManager = Context.get().autowire(new TaskManager());
		return taskManager;
	}

	private EntityStore entityStore;

	public final EntityStore getEntityStore() {
		if (entityStore == null) entityStore = createEntityStore();
		return entityStore;
	}

	protected EntityStore createEntityStore() {
		FileEntityStore store = new FileEntityStore();
		store.setDir(getApplicationDataDir() + "/entities");
		File backupDir = new File(getApplicationDataDir() + "/entities-rescue");

		File backupDirOld = new File(getApplicationDataDir() + "/backup/entities");
		if (backupDirOld.exists()) {
			backupDirOld.renameTo(backupDir);
			backupDirOld.delete();
			backupDirOld.getParentFile().delete();
		}

		store.setBackupDir(backupDir.getPath());
		store.setVersion(getDataVersion());
		Context.get().autowire(store);

		store.deleteOldBackups();

		return store;
	}

	protected int getDataVersion() {
		return -1;
	}

	private XStreamSerializer beanSerializer;

	public final Serializer getBeanSerializer() {
		if (beanSerializer == null) {
			beanSerializer = new XStreamSerializer();
			Context.get().autowire(beanSerializer);
		}
		return beanSerializer;
	}

	private DaoService daoService;

	public final DaoService getDaoService() {
		if (daoService == null) {
			daoService = new DaoService();
			Context.get().autowire(daoService);
			daoService.initialize(context);
			for (DaoListener listener : Context.get().getBeansByType(DaoListener.class))
				daoService.addListener(listener);
		}
		return daoService;
	}

	private TransactionService transactionService;

	public final TransactionService getTransactionService() {
		if (transactionService == null) {
			transactionService = new TransactionService();
			Context.get().autowire(transactionService);
		}
		return transactionService;
	}

}
