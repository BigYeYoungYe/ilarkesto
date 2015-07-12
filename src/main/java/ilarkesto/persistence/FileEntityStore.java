/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>
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
package ilarkesto.persistence;

import ilarkesto.core.base.Str;
import ilarkesto.core.logging.Log;
import ilarkesto.core.persistance.AEntityQuery;
import ilarkesto.core.persistance.AllByTypeQuery;
import ilarkesto.core.time.Date;
import ilarkesto.io.IO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class FileEntityStore implements EntityStore {

	private static final Log log = Log.get(FileEntityStore.class);

	public static String CLUSTER_FILE_NAME = "cluster.xml";

	private boolean versionSaved;
	private boolean versionChecked;
	private boolean locked;

	private ThreadLocal<Transaction> threadLocalTransaction = new ThreadLocal<Transaction>();

	// --- dependencies ---

	private long version;

	@Override
	public void setVersion(long version) {
		this.version = version;
	}

	private Serializer beanSerializer;

	public void setBeanSerializer(Serializer beanSerializer) {
		this.beanSerializer = beanSerializer;
	}

	private EntityfilePreparator entityfilePreparator;

	public void setEntityfilePreparator(EntityfilePreparator entityfilePreparator) {
		this.entityfilePreparator = entityfilePreparator;
	}

	private String dir;

	public void setDir(String dir) {
		this.dir = dir;
	}

	private String backupDir;

	public void setBackupDir(String backupDir) {
		this.backupDir = backupDir;
	}

	// --- ---

	public FileEntityStore() {
		Transaction.backend = this;
	}

	@Override
	public void onEntityModified() {}

	@Override
	public synchronized void lock() {
		if (locked) return;
		locked = true;
		log.info("File entity store locked.");
	}

	@Override
	public Transaction getTransaction() {
		Transaction t = threadLocalTransaction.get();
		if (t == null) {
			t = new Transaction();
			log.debug("Transaction created: " + t);
			threadLocalTransaction.set(t);
		}
		return t;
	}

	@Override
	public void onTransactionFinished(Transaction transaction) {
		threadLocalTransaction.set(null);
	}

	@Override
	public void update(Collection<AEntity> modified, Collection<String> deletedIds,
			Map<String, Map<String, String>> modifiedPropertiesByEntityId, Runnable callback) {
		if (locked) throw new RuntimeException("Can not persist entity changes. EntityStore already locked.");

		if (!versionSaved) saveVersion();

		// create operations
		List<Operation> operations = new ArrayList<FileEntityStore.Operation>(modified.size() + deletedIds.size());
		for (AEntity entity : modified) {
			operations.add(new SaveOperation(entity));
		}

		for (String deletedId : deletedIds) {
			operations.add(new DeleteOperation(getById(deletedId)));
		}

		// prepare operations
		for (Operation operation : operations) {
			operation.prepare();
		}

		// complete operations (critical)
		for (Operation operation : operations) {
			operation.complete();
		}

		StringBuilder sb = new StringBuilder();
		for (Operation operation : operations) {
			sb.append("\n    ").append(operation.toString());
		}
		log.debug("Entity changes persisted.", sb.toString());

		if (callback != null) callback.run();
	}

	private Map<String, AEntity> getDao(Class<? extends AEntity> type) {
		Map<String, AEntity> dao = entitiesByIdByType.get(type);
		if (dao == null) { throw new RuntimeException("Unknown entity type: " + type); }
		return dao;
	}

	@Override
	public AEntity getById(String id) {
		for (Map.Entry<Class<AEntity>, Map<String, AEntity>> daoEntry : entitiesByIdByType.entrySet()) {
			AEntity entity = daoEntry.getValue().get(id);
			if (entity != null) return entity;
		}
		return null;
	}

	@Override
	public boolean containsWithId(String id) {
		for (Map<String, AEntity> entitiesById : entitiesByIdByType.values()) {
			if (entitiesById.containsKey(id)) return true;
		}
		return false;
	}

	@Override
	public AEntity findFirst(AEntityQuery<AEntity> query) {
		for (Map.Entry<Class<AEntity>, Map<String, AEntity>> daoEntry : entitiesByIdByType.entrySet()) {
			if (!query.testType(daoEntry.getKey())) continue;

			for (AEntity entity : daoEntry.getValue().values()) {
				if (query.test(entity)) return entity;
			}
		}
		return null;
	}

	@Override
	public <C extends Collection<AEntity>> C findAll(AEntityQuery<AEntity> query, C resultCollection) {
		for (Map.Entry<Class<AEntity>, Map<String, AEntity>> entry : entitiesByIdByType.entrySet()) {
			if (!query.testType(entry.getKey())) continue;
			if (query.getClass().equals(AllByTypeQuery.class)) {
				resultCollection.addAll(entry.getValue().values());
			} else {
				for (AEntity entity : entry.getValue().values()) {
					if (query.test(entity)) resultCollection.add(entity);
				}
			}
		}
		return resultCollection;
	}

	@Override
	public List<AEntity> getByIdsAsList(Collection<String> ids) {
		return getByIds(ids, new ArrayList<AEntity>(ids.size()));
	}

	@Override
	public Set<AEntity> getByIdsAsSet(Collection<String> ids) {
		return getByIds(ids, new HashSet<AEntity>(ids.size()));
	}

	@Override
	public <C extends Collection<AEntity>> C getByIds(Collection<String> ids, C resultContainer) {
		for (Map.Entry<Class<AEntity>, Map<String, AEntity>> entry : entitiesByIdByType.entrySet()) {
			Map<String, AEntity> entites = entry.getValue();
			for (String id : ids) {
				AEntity entity = entites.get(id);
				if (entity != null) resultContainer.add(entity);
			}
		}
		return resultContainer;
	}

	private Map<Class, String> aliases = new HashMap<Class, String>();

	private Map<Class<AEntity>, Map<String, AEntity>> entitiesByIdByType = new HashMap<Class<AEntity>, Map<String, AEntity>>();

	@Override
	public void setAlias(String alias, Class cls) {
		aliases.put(cls, alias);
		beanSerializer.setAlias(alias, cls);
	}

	@Override
	public void load(Class<? extends AEntity> cls, String alias, boolean deleteOnFailure) {
		if (!versionChecked) checkVersion();

		aliases.put(cls, alias);

		Map<String, AEntity> entities = new HashMap<String, AEntity>();
		entitiesByIdByType.put((Class<AEntity>) cls, entities);

		beanSerializer.setAlias(Str.lowercaseFirstLetter(alias), cls);
		beanSerializer.setAlias(alias, cls);

		File entitiesDir = new File(dir + "/" + Str.lowercaseFirstLetter(alias));

		File clusterFile = new File(dir + "/" + CLUSTER_FILE_NAME);
		if (clusterFile.exists()) {
			loadCluster(clusterFile, entities, cls, alias);
		}

		File[] files = entitiesDir.listFiles();
		int count = files == null ? 0 : files.length;
		log.info("Loading", count, "entitiy files:", alias);
		if (count > 0) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String filename = file.getName();

				if (filename.equals(CLUSTER_FILE_NAME)) continue;
				if (!filename.endsWith(".xml")) {
					log.warn("Unsupported file. Skipping:", filename);
					continue;
				}

				try {
					loadObject(file, entities, cls, alias);
				} catch (Exception ex) {
					if (!deleteOnFailure) throw new RuntimeException("Loading object from " + file + " failed", ex);
					log.warn("Loading object from file failed:", file, ex);
					if (true) { // delete
						backup(file, alias);
						file.delete();
					}
				}
			}
		}
	}

	private void loadCluster(File file, Map<String, AEntity> container, Class type, String alias) {
		if (entityfilePreparator != null) entityfilePreparator.prepareClusterfile(file, type, alias);

		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		Collection<AEntity> entities = (Collection<AEntity>) beanSerializer.deserialize(in);

		for (AEntity entity : entities) {
			container.put(entity.getId(), entity);
		}

		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void loadObject(File file, Map<String, AEntity> container, Class type, String alias) {
		if (entityfilePreparator != null) entityfilePreparator.prepareEntityfile(file, type, alias);

		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		AEntity entity = (AEntity) beanSerializer.deserialize(in);
		container.put(entity.getId(), entity);
		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void backup(File src, String type) {
		if (src.isDirectory()) throw new RuntimeException("sorry, backing up directories is not implemented yet.");

		String destinationPath = backupDir + "/" + Date.today() + "/" + type + "/";
		File dst = new File(destinationPath + src.getName());
		for (int i = 2; dst.exists(); i++) {
			dst = new File(destinationPath + i + "_" + src.getName());
		}

		// LOG.debug("Backing up", src.getPath(), "to", dst.getPath());
		IO.copyFile(src.getPath(), dst.getPath());
	}

	private synchronized void checkVersion() {
		versionChecked = true;
		if (version <= 0) return;
		File propertiesFile = getPropertiesFile();
		if (!propertiesFile.exists()) return;
		Properties properties = IO.loadProperties(propertiesFile, IO.UTF_8);
		String s = properties.getProperty("version");
		if (Str.isBlank(s)) return;
		long dataVersion = Long.parseLong(s);
		if (dataVersion > version)
			throw new IllegalStateException("Data stored in " + dir
					+ " was created by a newer version of the application. "
					+ "You have probably downgraded. Since data formats changed, this is not possible. "
					+ "Application version is " + version + ", data version is " + dataVersion + ".");
	}

	private synchronized void saveVersion() {
		versionSaved = true;
		if (version <= 0) return;
		File propertiesFile = getPropertiesFile();
		Properties properties = propertiesFile.exists() ? IO.loadProperties(propertiesFile, IO.UTF_8)
				: new Properties();
		properties.setProperty("version", String.valueOf(version));
		IO.saveProperties(properties, getClass().getName(), propertiesFile);
	}

	@Override
	public void deleteOldBackups() {
		if (Str.isBlank(backupDir)) return;
		File[] dirs = new File(backupDir).listFiles();
		if (dirs == null || dirs.length == 0) return;
		Date deadline = Date.beforeDays(7);
		log.info("Deleting temporary entity backups from before", deadline);
		for (File dir : dirs) {
			if (!dir.isDirectory()) continue;
			String name = dir.getName();
			Date date = null;
			try {
				date = new Date(name);
			} catch (Throwable ex) {
				continue;
			}
			if (date.isBefore(deadline)) {
				log.debug("    Deleting temporary enity backups:", name);
				IO.delete(dir);
			}
		}
	}

	private File getPropertiesFile() {
		return new File(dir + "/store.properties");
	}

	abstract class Operation {

		protected abstract void prepare();

		protected abstract void complete();

		protected AEntity entity;

		public Operation(AEntity entity) {
			this.entity = entity;
		}

	}

	class SaveOperation extends Operation {

		private File tmpFile;
		private File file;

		public SaveOperation(AEntity entity) {
			super(entity);
		}

		@Override
		protected void prepare() {
			tmpFile = new File(dir + "/tmp/" + entity.getId() + ".xml");
			file = new File(dir + "/" + Str.lowercaseFirstLetter(entity.getDao().getEntityName()) + "/"
					+ entity.getId() + ".xml");

			wirteTemporaryFile();
			backupExistingFile();
		}

		@Override
		protected void complete() {
			IO.move(tmpFile, file, true);
			getDao(entity.getClass()).put(entity.getId(), entity);
		}

		public void backupExistingFile() {
			if (file.exists() && !(entity instanceof BackupHostile)) {
				backup(file, entity.getDao().getEntityName());
			}
		}

		public void wirteTemporaryFile() {
			if (!tmpFile.getParentFile().exists()) tmpFile.getParentFile().mkdirs();
			BufferedOutputStream out;
			try {
				out = new BufferedOutputStream(new FileOutputStream(tmpFile));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			beanSerializer.serialize(entity, out);
			IO.close(out);

			if (!tmpFile.exists()) throw new RuntimeException("Writing entity file failed: " + tmpFile.getPath());

			if (tmpFile.length() < 1)
				throw new RuntimeException("Writing entity file caused empty file: " + tmpFile.getPath());
		}

		@Override
		public String toString() {
			return "SAVED " + file.getPath() + " -> " + entity;
		}

	}

	class DeleteOperation extends Operation {

		private File file;

		public DeleteOperation(AEntity entity) {
			super(entity);
		}

		@Override
		protected void prepare() {
			file = new File(dir + "/" + entity.getDao().getEntityName() + "/" + entity.getId() + ".xml");

			// backup
			if (file.exists() && !(entity instanceof BackupHostile)) {
				backup(file, entity.getDao().getEntityName());
			}

		}

		@Override
		protected void complete() {
			IO.delete(file);
			getDao(entity.getClass()).remove(entity.getId());
		}

		@Override
		public String toString() {
			return "DELETED " + file.getPath();
		}

	}

}
