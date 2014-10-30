package ilarkesto.net;

import ilarkesto.base.Reflect;
import ilarkesto.core.logging.Log;
import ilarkesto.io.IO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class HttpDownloader {

	public static Class<? extends HttpDownloader> defaultType = ApacheHttpDownloader.class;

	private static final Log log = Log.get(HttpDownloader.class);

	private static final int DEFAULT_REDIRECTS = 3;

	private String username;
	private String password;
	private String baseUrl;

	public static HttpDownloader create() {
		return Reflect.newInstance(defaultType);
	}

	public boolean isInternetAvailable() {
		return true;
	}

	public String post(String url, Map<String, String> parameters, String charset) {
		return post(url, parameters, null, charset);
	}

	public String post(String url, Map<String, String> parameters, Map<String, String> requestHeaders, String charset) {
		if (requestHeaders != null && !requestHeaders.isEmpty())
			throw new IllegalArgumentException("request headers not supported with " + getClass().getName());
		return IO.postAndGetResult(url, parameters, charset, null, null);
	}

	public final void downloadUrlToFile(String url, File file) {
		downloadUrlToFile(url, file, DEFAULT_REDIRECTS);
	}

	public void downloadUrlToFile(String url, File file, int followRedirects) {
		IO.downloadUrlToFile(url, file.getPath());
	}

	public void downloadZipAndExtract(String url, File dir) {
		url = getFullUrl(url);
		log.info(url);
		InputStream is = null;
		ZipInputStream zis = null;
		try {
			is = IO.openUrlInputStream(url, username, password);
			zis = new ZipInputStream(new BufferedInputStream(is));
			ZipEntry ze;
			while (true) {
				try {
					ze = zis.getNextEntry();
				} catch (IOException ex) {
					throw new RuntimeException("Extracting zip file failed: " + url, ex);
				}
				if (ze == null) break;
				File file = new File(dir.getPath() + "/" + ze.getName());
				IO.createDirectory(file.getParentFile());
				BufferedOutputStream out;
				try {
					out = new BufferedOutputStream(new FileOutputStream(file));
				} catch (FileNotFoundException ex) {
					throw new RuntimeException("Writing file failed:" + file, ex);
				}
				IO.copyData(zis, out);
				IO.close(out);
			}
		} finally {
			IO.closeQuiet(zis);
			IO.closeQuiet(is);
		}
	}

	public final String downloadText(String url, String charset) {
		return downloadText(url, charset, DEFAULT_REDIRECTS);
	}

	public String downloadText(String url, String charset, int followRedirects) throws HttpRedirectException {
		url = getFullUrl(url);
		log.info(url);
		return IO.downloadUrlToString(url, charset, username, password);
	}

	private String getFullUrl(String url) {
		if (baseUrl == null) return url;
		return baseUrl + url;
	}

	public HttpDownloader setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	public HttpDownloader setAuthentication(String username, String password) {
		this.username = username;
		this.password = password;
		return this;
	}

	// ---

	public static class HttpRedirectException extends RuntimeException {

		public HttpRedirectException(String location) {
			super(location);
		}

		public String getLocation() {
			return getMessage();
		}

	}
}
