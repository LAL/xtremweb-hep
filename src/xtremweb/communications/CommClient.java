/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 * 
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.communications;

/**
 * CommClient.java
 *
 *
 * Created: Jun 2nd, 2005
 *
 * @author Oleg Lodygensky
 * @since RPCXW
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.net.ssl.SSLHandshakeException;

import org.xml.sax.SAXException;

import xtremweb.common.AppInterface;
import xtremweb.common.Cache;
import xtremweb.common.DataInterface;
import xtremweb.common.GroupInterface;
import xtremweb.common.HostInterface;
import xtremweb.common.Logger;
import xtremweb.common.LoggerLevel;
import xtremweb.common.MileStone;
import xtremweb.common.SessionInterface;
import xtremweb.common.StatusEnum;
import xtremweb.common.Table;
import xtremweb.common.TaskInterface;
import xtremweb.common.TraceInterface;
import xtremweb.common.UID;
import xtremweb.common.UserGroupInterface;
import xtremweb.common.UserInterface;
import xtremweb.common.Version;
import xtremweb.common.WorkInterface;
import xtremweb.common.WorkerParameters;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLReader;
import xtremweb.common.XMLValue;
import xtremweb.common.XMLVector;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.security.XWAccessRights;

public abstract class CommClient implements ClientAPI {

	private final Logger logger;

	/**
	 * @since 7.0.0
	 */
	public void setLoggerLevel(LoggerLevel l) {
		logger.setLoggerLevel(l);
	}

	/**
	 * This is the cache where download object are cached
	 */
	private static Cache cache = null;
	/**
	 * This hashtable stores known communication handlers. Keys are
	 * communications schemes; values are CommClient objects.<br />
	 * This is automatically initialized with the tuple:<br />
	 * (xtremweb.communications.Connection.XWSCHEME,
	 * xtremweb.common.XWPropertyDefs.COMMLAYER)<br />
	 * This is then filled as needs
	 * 
	 * @see #getClient(URI)
	 * @see xtremweb.communications.Connection#XWSCHEME
	 * @see xtremweb.common.XWPropertyDefs#COMMHANDLERS
	 */
	private static Hashtable commHandlers = null;

	/**
	 * This contains configuration
	 */
	private static XWConfigurator config = null;

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * This is max amount of times we try on socket time out exception
	 * 
	 * @since 7.4.0
	 */
	private static int socketRetries;

	/**
	 * @return the soretries
	 */
	public static int getSocketRetries() {
		return socketRetries;
	}

	/**
	 * This aims to display some time stamps
	 */
	private final MileStone mileStone;

	protected void mileStone(String msg) {
		mileStone.println(msg);
	}

	/**
	 * This retrieve this client port number
	 * 
	 * @since 5.9.0
	 */
	public abstract int getPort();

	/**
	 * This creates a new URI to connect to current dispatcher The URI path
	 * contains the UID (e.g. xw://monserver; xw://monserver/uid)
	 * 
	 * @param uid
	 *            is the uid used as path if the returned uri, if not null
	 * @see xtremweb.communications.URI#URI(String, UID)
	 */
	public URI newURI(UID uid) throws URISyntaxException {
		logger.finest("CommClient#newURI " + config.getCurrentDispatcher()
				+ ":" + getPort() + "/" + uid);
		final URI ret = new URI(config.getCurrentDispatcher(), uid);
		return ret;
	}

	/**
	 * This call newURI(null)
	 * 
	 * @see #newURI(UID)
	 */
	public URI newURI() throws URISyntaxException {
		return newURI((UID) null);
	}

	/**
	 * This opens communications channel
	 */
	protected abstract void open(URI u) throws UnknownHostException,
	NoRouteToHostException, SSLHandshakeException,
	SocketTimeoutException, IOException;

	/**
	 * This closes communications channel
	 */
	public abstract void close();

	/**
	 * This tells whether communication channel should be automatically closed
	 * or if this is left to the entity using this CommClient. Default is true :
	 * communications are automatically closed
	 * 
	 * @since 7.4.0
	 */
	private boolean autoClose = true;

	/**
	 * @return the autoClose
	 */
	public boolean isAutoClose() {
		return autoClose;
	}

	/**
	 * This sets autoClose attribute
	 * 
	 * @since 7.4.0
	 */
	public void setAutoClose(boolean c) {
		autoClose = c;
	}

	/**
	 * this tells if communication channel is opened
	 */
	private boolean opened = false;

	/**
	 * @return the opened
	 */
	public boolean isOpened() {
		return opened;
	}

	/**
	 * @param opened
	 *            the opened to set
	 */
	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	/*
	 * this contains the amount of messages already sent
	 * 
	 * @see util#MAXMESSAGES
	 */
	private int nbMessages = 0;

	/**
	 * @return the nbMessages
	 */
	public int getNbMessages() {
		return nbMessages;
	}

	/**
	 * @param nbMessages
	 *            the nbMessages to set
	 */
	public void setNbMessages(int nbMessages) {
		this.nbMessages = nbMessages;
	}

	/**
	 * This writes an object to output channel
	 */
	protected abstract void write(XMLRPCCommand cmd) throws IOException;

	/**
	 * This is the default constructor; this set connectionless to true, opened
	 * to false
	 */
	protected CommClient() {
		logger = new Logger(this);
		mileStone = new MileStone(getClass());
		nbMessages = 0;
		autoClose = true;
		opened = false;
	}

	/**
	 * This does nothing if config is already set. Otherwise this calls
	 * changeConfig(c)
	 * 
	 * @see #changeConfig(XWConfigurator)
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 */
	public static void setConfig(XWConfigurator c) throws IOException {
		if (config != null) {
			return;
		}
		changeConfig(c);
	}

	/**
	 * This configures this object : this extracts communication layers from
	 * config file and instantiates communication handlers accordingly. This
	 * also sets the cache
	 * 
	 * @param c
	 *            is the configuration
	 * @exception IOException
	 *                is thrown if cache directory can not be created
	 */
	public static void changeConfig(XWConfigurator c) throws IOException {

		config = c;

		cache = new Cache(config);

		commHandlers = new Hashtable();

		final Hashtable layers = (Hashtable) XWTools.hash(config
				.getProperty(XWPropertyDefs.COMMHANDLERS));
		final Enumeration enumLayers = layers.keys();

		socketRetries = config.getInt(XWPropertyDefs.SORETRIES);

		while (enumLayers.hasMoreElements()) {

			final String key = (String) enumLayers.nextElement();
			final String value = (String) layers.get(key);

			try {
				addHandler(key, value);
			} catch (final Exception e) {
				System.err.println("Init comm layers: ignoring (" + key + ", "
						+ value + ") : " + e);
			}
		}

		try {
			if (commHandlers.get(Connection.xwScheme()) == null) {
				final String defaultLayer = config
				.getProperty(XWPropertyDefs.COMMLAYER);

				addHandler(Connection.xwScheme(), defaultLayer);
			}
			if (commHandlers.get(Connection.xwsScheme()) == null) {
				addHandler(Connection.xwsScheme(),
						config.getProperty(XWPropertyDefs.COMMLAYER));
			}
			if (commHandlers.get(Connection.httpScheme()) == null) {
				addHandler(Connection.httpScheme(), Connection.HTTPPORT.layer());
			}
			if (commHandlers.get(Connection.httpsScheme()) == null) {
				addHandler(Connection.httpsScheme(),
						Connection.HTTPPORT.layer());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			XWTools.fatal("init comm layers : " + e);
		}
	}

	/**
	 * This clears the cache
	 */
	public void clearCache() {
		cache.clear();
	}

	/**
	 * This adds or replaces a comm handler
	 */
	public static void addHandler(String schema, String className) {

		commHandlers.put(schema, className);
	}

	/**
	 * This constructs an object accordingly to the URI scheme; the URI schema
	 * defines the object type
	 * 
	 * @see #commHandlers
	 * @param scheme
	 *            is the URI scheme to retrieve client for
	 * @throws InstantiationException
	 *             if the class referred by the scheme can not be instantiated
	 */
	public static CommClient getClient(String scheme)
	throws InstantiationException {

		if (scheme == null) {
			throw new InstantiationException("scheme is null");
		}
		if (commHandlers == null) {
			throw new InstantiationException("Comm Handlers not initialized");
		}
		try {
			return (CommClient) (Class.forName((String) commHandlers
					.get(scheme))).newInstance();
		} catch (final IllegalAccessException e) {
			throw new InstantiationException(e.getMessage());
		} catch (final ClassNotFoundException e) {
			throw new InstantiationException(e.getMessage());
		}
	}

	/**
	 * This calls getClient(uri.getScheme())
	 * 
	 * @see #commHandlers
	 * @see #getClient(String)
	 * @param uri
	 *            is the URI to connect to
	 * @throws InstantiationException
	 *             if the class referred by the scheme can not be instantiated
	 */
	public static CommClient getClient(URI uri) throws InstantiationException {
		return getClient(uri.getScheme());
	}

	/**
	 * This retrieves the configuration
	 * 
	 * @return this client configuration
	 */
	public XWConfigurator getConfig() {
		return config;
	}

	/**
	 * This sends an XMLRPC command to be executed on server side This uses
	 * config._user, if cmd.getUser == null Since 2.2.0, all command needs an
	 * answer : we don't close comm channel here
	 * 
	 * @param cmd
	 *            is the command to send
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws SocketTimeoutException
	 * @throws SSLHandshakeException
	 * @throws NoRouteToHostException
	 */
	public void sendCommand(XMLRPCCommand cmd) throws NoRouteToHostException,
	SSLHandshakeException, SocketTimeoutException,
	UnknownHostException, IOException {
		try {
			mileStone("<sendCommand idrpc='" + cmd.getIdRpc().toString() + "'>");

			open(cmd.getURI());

			if (cmd.getUser() == null) {
				cmd.setUser(config.getUser());
			}

			logger.finest("sendCommand " + cmd.toXml());

			write(cmd);
			++nbMessages;
		} finally {
			mileStone("</sendCommand>");
		}
	}

	/**
	 * This sends (creates or updates) an object definition
	 * 
	 * @since 5.0.0
	 */
	public void send(XMLRPCCommandSend command) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {
		try {
			sendCommand(command);
			newXMLVector();
		} finally {
			close();
		}
	}

	/**
	 * This writes a file to output channel
	 */
	public abstract void writeFile(File f) throws IOException;

	/**
	 * This reads a file from input channel
	 */
	public abstract void readFile(File f) throws IOException;

	/**
	 * This creates an object from channel
	 */
	protected abstract AppInterface newAppInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract DataInterface newDataInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract Table newTableInterface() throws InvalidKeyException,
	AccessControlException, IOException, SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract GroupInterface newGroupInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract HostInterface newHostInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract SessionInterface newSessionInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract TaskInterface newTaskInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract TraceInterface newTraceInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract UserInterface newUserInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract UserGroupInterface newUserGroupInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract WorkInterface newWorkInterface()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract Version newXMLVersion() throws InvalidKeyException,
	AccessControlException, IOException, SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract XMLVector newXMLVector() throws InvalidKeyException,
	AccessControlException, IOException, SAXException;

	/**
	 * This creates an object from channel
	 */
	protected abstract XMLHashtable newXMLHashtable()
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException;

	/**
	 * This creates an object from channel
	 */
	protected Table newTableInterface(InputStream in)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		final BufferedInputStream input = new BufferedInputStream(in);

		try {
			input.mark(XWTools.BUFFEREND);
			return newWorkInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newTaskInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newAppInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newDataInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newUserInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newGroupInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newSessionInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newHostInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newUserGroupInterface(input);
		} catch (final SAXException e) {
		}
		try {
			input.reset();
			input.mark(XWTools.BUFFEREND);
			return newTraceInterface(input);
		} catch (final SAXException e) {
		}
		throw new IOException(
		"Unable to create new Interface from input stream");
	}

	/**
	 * This creates an object from channel
	 */
	protected <T extends Table> T newTableInterface(T itf, InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		try {
			mileStone("<newTableInterface itf='" + itf.getClass() + "'>");
			final XMLReader reader = new XMLReader(itf);
			reader.read(input);
		} finally {
			mileStone("</newTableInterface>");
		}
		return itf;
	}

	/**
	 * This creates an object from channel
	 */
	protected AppInterface newAppInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final AppInterface itf = new AppInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected DataInterface newDataInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final DataInterface itf = new DataInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected GroupInterface newGroupInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final GroupInterface itf = new GroupInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected HostInterface newHostInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final HostInterface itf = new HostInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected SessionInterface newSessionInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final SessionInterface itf = new SessionInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected TaskInterface newTaskInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final TaskInterface itf = new TaskInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected TraceInterface newTraceInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final TraceInterface itf = new TraceInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected UserInterface newUserInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final UserInterface itf = new UserInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected UserGroupInterface newUserGroupInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final UserGroupInterface itf = new UserGroupInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected WorkInterface newWorkInterface(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		final WorkInterface itf = new WorkInterface();
		return newTableInterface(itf, input);
	}

	/**
	 * This creates an object from channel
	 */
	protected XMLVector newXMLVector(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		try {
			mileStone("<newXMLVector>");
			final XMLVector ret = new XMLVector();
			final XMLReader reader = new XMLReader(ret);
			reader.read(input);
			return ret;
		} finally {
			mileStone("</newXMLVector>");
		}
	}

	/**
	 * This creates an object from channel
	 */
	protected Version newXMLVersion(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		try {
			mileStone("<newXMLVersion>");
			final Version ret = new Version();
			final XMLReader reader = new XMLReader(ret);
			reader.read(input);
			return ret;
		} finally {
			mileStone("</newXMLVersion>");
		}
	}

	/**
	 * This creates an object from channel
	 */
	protected XMLHashtable newXMLHashtable(InputStream input)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		try {
			mileStone("<newXMLHashtable>");
			final XMLHashtable ret = new XMLHashtable();
			final XMLReader reader = new XMLReader(ret);
			reader.read(input);
			return ret;
		} finally {
			mileStone("</newXMLHashtable>");
		}
	}

	/**
	 * This disconnects this client from current dispatcher. Disconnection
	 * removes sessions from server
	 */
	public void disconnect() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		disconnect(new XMLRPCCommandDisconnect(newURI(), config.getUser()));
	}

	/**
	 * This disconnects this client. Disconnection removes sessions from server
	 */
	public void disconnect(XMLRPCCommandDisconnect command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		try {
			sendCommand(command);
			newXMLVector();
		} finally {
			close();
		}
	}

	/**
	 * This adds an URI to cache to be able to manage URL too
	 */
	public void addToCache(URI uri) throws IOException {
		cache.add(uri);
	}

	/**
	 * This sets the content file in cache for the given URI
	 * 
	 * @since 8.0.1
	 */
	public void setContentFile(URI uri, File f) throws IOException {
		cache.setContentFile(uri, f);
	}

	/**
	 * This adds an URI to cache to be able to manage URL too
	 * 
	 * @since 4.2.0
	 */
	public void addToCache(Table itf, URI uri) throws IOException {
		cache.add(itf, uri);
	}

	/**
	 * This retrieves content file for the given UID The object must be already
	 * cached
	 * 
	 * @return a File where to store content for the cached object, null
	 *         otherwise
	 */
	public File getContentFile(URI uri) throws IOException {
		return cache.getContentFile(uri);
	}

	/**
	 * This locks an entry in the cache. The object must be already cached.
	 * 
	 * @since 7.0.0
	 * @see Cache#unlock(URI)
	 */
	public void lock(URI uri) throws IOException {
		cache.lock(uri);
	}

	/**
	 * This unlocks an entry in the cache.
	 * 
	 * @since 7.0.0
	 * @see Cache#lock(URI)
	 */
	public void unlock(URI uri) throws IOException {
		cache.unlock(uri);
	}

	/**
	 * This retrieves server version
	 * 
	 * @return a Version object
	 * @since 5.6.1
	 */
	public Version version() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		return version(new XMLRPCCommandVersion(newURI()));
	}

	/**
	 * This retrieves server version
	 * 
	 * @param command
	 *            is the VERSION command to send to server
	 * @return a Version object
	 * @since 5.6.1
	 */
	public Version version(XMLRPCCommandVersion command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		Version version = null;
		try {
			sendCommand(command);
			version = newXMLVersion();
		} finally {
			close();
		}
		return version;
	}

	/**
	 * This calls get(newURI(uid))
	 * 
	 * @param uid
	 *            is the UID of the object to retreive
	 * @see #get(URI)
	 * @since 1.1.0
	 */
	public Table get(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		final URI uri = newURI(uid);
		return get(uri);
	}

	/**
	 * This calls get(uri, true)
	 * 
	 * @param uri
	 *            is the URI to get the object from
	 * @see #get(URI, boolean)
	 * @since 4.2.0
	 */
	public Table get(URI uri) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		return get(uri, false);
	}

	/**
	 * This calls get(new XMLRPCCommandGet(uri), bypass)
	 * 
	 * @param uid
	 *            is the UID of the object to retreive
	 * @param bypass
	 *            tells to force download
	 * @see #get(URI, boolean)
	 * @since 1.0.0
	 */
	public Table get(UID uid, boolean bypass) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		final URI uri = newURI(uid);
		return get(uri, bypass);
	}

	/**
	 * This calls get(new XMLRPCCommandGet(uri), bypass)
	 * 
	 * @param uri
	 *            is the URI of the object to get
	 * @param bypass
	 *            tells to force download
	 * @see #get(XMLRPCCommandGet, boolean)
	 * @since 4.2.0
	 */
	public Table get(URI uri, boolean bypass) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final XMLRPCCommandGet cmd = new XMLRPCCommandGet(uri);
		return get(cmd, bypass);
	}

	/**
	 * This calls get(command, true)
	 * 
	 * @param command
	 *            is the GET command to send to server
	 * @see #get(XMLRPCCommandGet, boolean)
	 * @since 1.0.0
	 */
	public Table get(XMLRPCCommandGet command) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {
		return get(command, true);
	}

	/**
	 * This retrieves an object definition given its URI, from server or from
	 * cache, if already in cache.
	 * 
	 * @param command
	 *            is the GET command to send to server
	 * @param bypass
	 *            if true object is downloaded from server even if already in
	 *            cache if false, object is only downloaded if not already in
	 *            cache
	 * @return an object definition
	 * @since 1.0.0
	 */
	public Table get(XMLRPCCommandGet command, boolean bypass)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		if (!bypass) {
			final Table object = cache.get(command.getURI());
			if (object != null) {
				return object;
			}
		}

		try {
			sendCommand(command);
			final Table object = newTableInterface();
			if (object != null) {
				cache.add(object, command.getURI());
			}
			return object;
		} finally {
			close();
		}
	}

	/**
	 * This calls get(new XMLRPCCommandGet(uri), bypass)
	 * 
	 * @param uid
	 *            is the UID of the object to retreive
	 * @see #get(URI, boolean)
	 * @since 1.0.0
	 */
	public Table getTask(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		return getTask(uid, false);
	}

	/**
	 * This calls get(new XMLRPCCommandGet(uri), bypass)
	 * 
	 * @param uid
	 *            is the UID of the object to retreive
	 * @param bypass
	 *            tells to force download
	 * @see #get(URI, boolean)
	 * @since 1.0.0
	 */
	public Table getTask(UID uid, boolean bypass) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		final URI uri = newURI(uid);
		return getTask(uri, bypass);
	}

	/**
	 * This calls get(uri, true)
	 * 
	 * @param uri
	 *            is the URI to get the object from
	 * @see #get(URI, boolean)
	 * @since 4.2.0
	 */
	public Table getTask(URI uri) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		return getTask(uri, false);
	}

	/**
	 * This calls get(new XMLRPCCommandGet(uri), bypass)
	 * 
	 * @param uri
	 *            is the URI of the object to get
	 * @param bypass
	 *            tells to force download
	 * @see #get(XMLRPCCommandGet, boolean)
	 * @since 4.2.0
	 */
	public Table getTask(URI uri, boolean bypass) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final XMLRPCCommandGetTask cmd = new XMLRPCCommandGetTask(uri);
		return getTask(cmd, bypass);
	}

	/**
	 * This calls get(command, true)
	 * 
	 * @param command
	 *            is the GET command to send to server
	 * @see #get(XMLRPCCommandGet, boolean)
	 * @since 1.0.0
	 */
	public Table getTask(XMLRPCCommandGetTask command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		return get(command, true);
	}

	/**
	 * This calls get(command, true) On Apr, 2012, we introduce this to easy
	 * client usage. This aims to retrieve task from either its UID **or** its
	 * WORKUID
	 * 
	 * @param command
	 *            is the GET command to send to server
	 * @see #get(XMLRPCCommandGet, boolean)
	 * @since 8.0.0
	 */
	public Table getTask(XMLRPCCommandGetTask command, boolean bypass)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		return get(command, bypass);
	}

	/**
	 * This calls chmod(new XMLRPCCommandChmod(uri), bypass)
	 * 
	 * @param uid
	 *            is the UID of the object to retreive
	 * @param r
	 *            is the access rights to apply to object
	 * @see #chmod(URI, XWAccessRights)
	 * @since 5.8.0
	 */
	public void chmod(UID uid, XWAccessRights r) throws InvalidKeyException,
	SAXException, AccessControlException, IOException,
	URISyntaxException {
		final URI uri = newURI(uid);
		chmod(uri, r);
	}

	/**
	 * This calls chmod(new XMLRPCCommandChmod(uri), bypass)
	 * 
	 * @param uri
	 *            is the URI of the object to chmod
	 * @param r
	 *            is the access rights to apply to object
	 * @see #chmod(XMLRPCCommandChmod)
	 * @since 5.8.0
	 */
	public void chmod(URI uri, XWAccessRights r) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {
		chmod(new XMLRPCCommandChmod(uri, r));
	}

	/**
	 * This retrieves an object definition given its URI, from server or from
	 * cache, if already in cache.
	 * 
	 * @param command
	 *            is the CHMOD command to send to server
	 * @since 5.8.0
	 */
	public void chmod(XMLRPCCommandChmod command) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {

		try {
			sendCommand(command);
			newXMLVector();
		} catch (final IllegalArgumentException e) {
			// this occurs if object does not exist on server side
			// because server then answers an empty XMLVector
		} finally {
			close();
		}
	}

	/**
	 * This sends (creates or updates) an application definition
	 */
	public void send(Table obj) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		final URI uri = newURI();
		final XMLRPCCommandSend cmd = new XMLRPCCommandSend(uri, obj);
		send(cmd);
	}

	/**
	 * This retrieves an application definition from server, given its name.
	 * Application is always downloaded from server, even if on already in cache
	 * 
	 * @param name
	 *            is the name of the application
	 * @return an application definition
	 * @since 2.0.0
	 */
	public AppInterface getApp(String name) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		return getApp(name, true);
	}

	/**
	 * This retrieves an application definition from server, given its name. If
	 * application is already in cache, it is not downloaded
	 * 
	 * @param name
	 *            is the name of the application
	 * @param bypass
	 *            if true app is downloaded from server even if already in cache
	 *            if false, app is only downloaded if not already in cache
	 * @return an application definition
	 * @since 2.0.0
	 */
	public AppInterface getApp(String name, boolean bypass)
	throws InvalidKeyException, AccessControlException, IOException,
	ClassNotFoundException, SAXException, URISyntaxException {

		if (!bypass) {
			final AppInterface app = cache.appByName(name);
			if (app != null) {
				return app;
			}
		}

		Vector<XMLValue> appVector = getApps().getXmlValues();
		try {
			logger.finest("commClient#getApp(" + name + ") vector.size = "
					+ appVector.size() + " " + appVector.toString());

			final Enumeration<XMLValue> myenum = appVector.elements();
			for (; myenum.hasMoreElements();) {

				AppInterface theApp = null;
				UID uid = (UID) myenum.nextElement().getValue();
				if (uid == null) {
					continue;
				}

				theApp = (AppInterface) get(uid);

				uid = null;

				if ((theApp != null)
						&& (theApp.getName().compareToIgnoreCase(name) == 0)) {
					return theApp;
				}
			}
		} finally {
			appVector.clear();
			appVector = null;
		}
		return null;
	}

	/**
	 * This retrieves all applications UID from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getApps() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final URI uri = newURI();
		final XMLRPCCommandGetApps cmd = new XMLRPCCommandGetApps(uri,
				config.getUser());
		return getApps(cmd);
	}

	/**
	 * This retrieves all applications UID from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getApps(XMLRPCCommandGetApps command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This removes an application from server
	 * 
	 * @param uri
	 *            is the URI of the object to remove
	 */
	public void remove(URI uri) throws IOException, InvalidKeyException,
	AccessControlException {
		remove(new XMLRPCCommandRemove(uri));
	}

	/**
	 * This removes an object definition from server
	 * 
	 * @param command
	 *            is the command to use
	 */
	public void remove(XMLRPCCommandRemove command) throws InvalidKeyException,
	AccessControlException, IOException {

		try {
			sendCommand(command);
			cache.remove(command.getURI());
			newXMLVector();
		} catch (final SAXException e) {
		} finally {
			close();
		}
	}

	/**
	 * This retrieves all datas UID from server
	 * 
	 * @return a vector of UIDs
	 * @since 2.0.0
	 */
	public XMLVector getDatas() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final URI uri = newURI();
		final XMLRPCCommandGetDatas cmd = new XMLRPCCommandGetDatas(uri,
				config.getUser());
		return getDatas(cmd);
	}

	/**
	 * This retrieves all datas UID from server
	 * 
	 * @return a vector of UIDs
	 * @since 2.0.0
	 */
	public XMLVector getDatas(XMLRPCCommandGetDatas command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This uploads a data content to server
	 * 
	 * @param uid
	 *            is the UID of the data to upload
	 * @param content
	 *            represents a File to get data to upload
	 * @since 2.0.0
	 */
	public void uploadData(UID uid, File content) throws InvalidKeyException,
	AccessControlException, IOException, URISyntaxException,
	SAXException {
		final URI uri = newURI(uid);
		uploadData(uri, content);
	}

	/**
	 * This uploads a data content to server
	 * 
	 * @param uri
	 *            is the data URI to upload
	 * @param content
	 *            represents a File to get data to upload
	 * @since 4.2.0
	 */
	public void uploadData(URI uri, File content) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {
		final XMLRPCCommandUploadData cmd = new XMLRPCCommandUploadData(uri);
		uploadData(cmd, content);
	}

	/**
	 * This uploads a data content to server
	 * 
	 * @param command
	 *            is the command to send to server
	 * @param content
	 *            represents a File to get data to upload
	 * @since 2.0.0
	 */
	public void uploadData(XMLRPCCommandUploadData command, File content)
	throws InvalidKeyException, AccessControlException, IOException {

		if (!content.exists()) {
			throw new IOException(content.getCanonicalPath() + " not found");
		}

		try {
			sendCommand(command);
			writeFile(content);
		} finally {
			try {
				newXMLVector();
			} catch (final SAXException e) {
			} finally {
				close();
			}
		}
	}

	/**
	 * This uploads a data to server from a file which name is the data UID
	 * 
	 * @param command
	 *            is the XMLRPC command
	 * @since 9.0.0
	 */
	public void uploadData(XMLRPCCommandUploadData command)
	throws InvalidKeyException, AccessControlException, IOException {
		uploadData(command, new File(command.getURI().getUID().toString()));
	}

	/**
	 * This downloads a data from server
	 * 
	 * @param uid
	 *            is the UID of the data to download
	 * @param content
	 *            represents a File to store downloaded data
	 * @since 2.0.0
	 */
	public void downloadData(UID uid, File content) throws InvalidKeyException,
	AccessControlException, IOException, URISyntaxException {
		final URI uri = newURI(uid);
		downloadData(uri, content);
	}

	/**
	 * This downloads a data from server
	 * 
	 * @param uri
	 *            is the URI of the data to download
	 * @param content
	 *            represents a File to store downloaded data
	 * @since 4.2.0
	 */
	public void downloadData(URI uri, File content) throws InvalidKeyException,
	AccessControlException, IOException {
		final XMLRPCCommandDownloadData cmd = new XMLRPCCommandDownloadData(uri);
		downloadData(cmd, content);
	}

	/**
	 * This downloads a data from server
	 * 
	 * @param command
	 *            is the XMLRPC command
	 * @param content
	 *            represents a File to store downloaded data
	 * @since 2.0.0
	 */
	public void downloadData(XMLRPCCommandDownloadData command, File content)
	throws InvalidKeyException, AccessControlException, IOException {
		try {
			sendCommand(command);
			readFile(content);
			newXMLVector();
		} catch (final SAXException e) {
		} finally {
			close();
		}
	}

	/**
	 * This downloads a data from server to file which name is the data UID
	 * 
	 * @param command
	 *            is the XMLRPC command
	 * @since 9.0.0
	 */
	public void downloadData(XMLRPCCommandDownloadData command)
	throws InvalidKeyException, AccessControlException, IOException {
		downloadData(command, new File(command.getURI().getUID().toString()));
	}

	/**
	 * This retrieves all groups UID from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getGroups() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final URI uri = newURI();
		final XMLRPCCommandGetGroups cmd = new XMLRPCCommandGetGroups(uri,
				config.getUser());
		return getGroups(cmd);
	}

	/**
	 * This retrieves all groups UID from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getGroups(XMLRPCCommandGetGroups command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This retrieves all works for the given group. This connect to current
	 * dispatcher
	 * 
	 * @param uid
	 *            is the UID of the group to retreive works for
	 * @return a Vector of UIDs
	 * @see #newURI(UID)
	 */
	public XMLVector getGroupWorks(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		final URI uri = newURI(uid);
		return getGroupWorks(uri);
	}

	/**
	 * This retrieves all works for the given group
	 * 
	 * @param uri
	 *            is the URI to connect to ; its path must contains the UID of
	 *            the group to retreive works for
	 * @return a Vector of UIDs
	 * @since 4.2.0
	 */
	public XMLVector getGroupWorks(URI uri) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {
		final XMLRPCCommandGetGroupWorks cmd = new XMLRPCCommandGetGroupWorks(
				uri);
		return getGroupWorks(cmd);
	}

	/**
	 * This retrieves all works for the given group
	 * 
	 * @return a Vector of UIDs
	 */
	public XMLVector getGroupWorks(XMLRPCCommandGetGroupWorks command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This retrieves all hosts from server
	 * 
	 * @return a vector of host UIDs
	 */
	public XMLVector getHosts() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetHosts cmd = new XMLRPCCommandGetHosts(uri,
				config.getUser());
		return getHosts(cmd);
	}

	/**
	 * This retrieves all hosts from server
	 * 
	 * @return a vector of host UIDs
	 */
	public XMLVector getHosts(XMLRPCCommandGetHosts command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * @deprecated this method is replaced by activateHost(UID, int) to conform
	 *             to method names
	 * @see #send(XMLRPCCommandSend)
	 */
	@Deprecated
	public void activateWorker(UID uid, boolean flag)
	throws InvalidKeyException, AccessControlException, IOException,
	URISyntaxException {
		activateHost(uid, flag);
	}

	/**
	 * Set host active flag.<br />
	 * 
	 * @param uid
	 *            is the host uid
	 * @param flag
	 *            is the active flag
	 */
	public void activateHost(UID uid, boolean flag) throws InvalidKeyException,
	AccessControlException, IOException, URISyntaxException {

		final URI uri = newURI(uid);
		final XMLRPCCommandActivateHost cmd = new XMLRPCCommandActivateHost(uri);
		activateHost(cmd);
	}

	/**
	 * Set host active flag.<br />
	 * 
	 * @param command
	 *            is the command to send to server
	 */
	public void activateHost(XMLRPCCommandActivateHost command)
	throws InvalidKeyException, AccessControlException, IOException {

		try {
			sendCommand(command);
			newXMLVector();
		} catch (final SAXException e) {
		} finally {
			close();
		}
	}

	/**
	 * This is not implemented yet
	 */
	public int setWorkersParameters(int nbWorkers, WorkerParameters p)
	throws InvalidKeyException, AccessControlException, IOException {
		throw new IOException(
		"TCPClient#setWorkersParameters is not implemented yet");
	}

	/**
	 * This is not implemented yet
	 */
	public WorkerParameters getWorkersParameters() throws IOException {
		throw new IOException(
		"TCPClient#getWorkersParameters is not implemented yet");
	}

	/**
	 * This is not implemented yet
	 */
	public int setWorkersNb(int nb) throws IOException {
		throw new IOException("TCPClient#setWorkersNb is not implemented yet");
	}

	/**
	 * This retrieves all sessions from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getSessions() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetSessions cmd = new XMLRPCCommandGetSessions(uri,
				config.getUser());
		return getSessions(cmd);
	}

	/**
	 * This retrieves all sessions from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getSessions(XMLRPCCommandGetSessions command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This retrieves all works for the given session
	 * 
	 * @param uid
	 *            is the session UID to retreive works for
	 * @return a Vector of UID
	 */
	public XMLVector getSessionWorks(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI(uid);
		final XMLRPCCommandGetSessionWorks cmd = new XMLRPCCommandGetSessionWorks(
				uri);
		return getSessionWorks(cmd);
	}

	/**
	 * This retrieves all works for the given session
	 * 
	 * @param command
	 *            is the command to send to server
	 * @return a Vector of UID
	 */
	public XMLVector getSessionWorks(XMLRPCCommandGetSessionWorks command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This retrieves all tasks from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getTasks() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetTasks cmd = new XMLRPCCommandGetTasks(uri,
				config.getUser());
		return getTasks(cmd);
	}

	/**
	 * This retrieves all tasks from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getTasks(XMLRPCCommandGetTasks command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This sends a work definition to server
	 * 
	 * @see #send(Table)
	 */
	public void submit(WorkInterface work) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {
		send(work);
	}

	/**
	 * This requests a work from server
	 * 
	 * @param h
	 *            describes the worker making this call
	 * @return a WorkInterface or null if no work available
	 */
	public WorkInterface workRequest(HostInterface h)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException, URISyntaxException {
		return workRequest(new XMLRPCCommandWorkRequest(newURI(), h));
	}

	/**
	 * This requests a work from server
	 * 
	 * @return a WorkInterface or null if no work available
	 */
	public WorkInterface workRequest(XMLRPCCommandWorkRequest command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		try {
			sendCommand(command);
			return newWorkInterface();
		} finally {
			close();
		}
	}

	/**
	 * This retrieves all works from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getWorks() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetWorks cmd = new XMLRPCCommandGetWorks(uri,
				config.getUser());
		return getWorks(cmd);
	}

	/**
	 * This retrieves all works from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getWorks(XMLRPCCommandGetWorks command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This broadcasts a new work to all workers
	 * 
	 * @param w
	 *            is the work to broadcast to all workers
	 */
	public void broadcast(WorkInterface w) throws InvalidKeyException,
	AccessControlException, IOException, URISyntaxException {
		broadcast(w.getUID());
	}

	/**
	 * This broadcasts a new work to all workers This connect to current
	 * dispatcher
	 * 
	 * @param uid
	 *            defines the UID of the work to broadcast
	 * @see #newURI(UID)
	 */
	public void broadcast(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, URISyntaxException {

		final URI uri = newURI(uid);
		broadcast(uri);
	}

	/**
	 * This broadcasts a new work to all workers
	 * 
	 * @param uri
	 *            is the URI to connect to ; its path must contains the UID of
	 *            the work to broadcast
	 * @since 4.2.0
	 */
	public void broadcast(URI uri) throws InvalidKeyException,
	AccessControlException, IOException {

		final XMLRPCCommandBroadcastWork cmd = new XMLRPCCommandBroadcastWork(
				uri);
		broadcast(cmd);
	}

	/**
	 * This broadcasts a new work to all workers
	 * 
	 * @param command
	 *            is the command to send to server
	 */
	public void broadcast(XMLRPCCommandBroadcastWork command)
	throws InvalidKeyException, AccessControlException, IOException {
		try {
			sendCommand(command);
			newXMLVector();
		} catch (final SAXException e) {
			logger.exception(e);
		} finally {
			close();
		}
	}

	/**
	 * This retrieves the SmartSockets hub address from server
	 * 
	 * @return an hashtable containing some parameters
	 */
	public XMLHashtable getHubAddress() throws InvalidKeyException,
	AccessControlException, IOException, SAXException {

		try {
			final XMLRPCCommandGetHubAddr cmd = new XMLRPCCommandGetHubAddr();
			sendCommand(cmd);
			return newXMLHashtable();
		} finally {
			close();
		}
	}

	/**
	 * This checks the provided work accordingly to the server status
	 * 
	 * @param uid
	 *            is the job UID
	 * @return an hashtable containing some parameters
	 */
	public XMLHashtable workAlive(final UID uid) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI(uid);
		final XMLRPCCommandWorkAliveByUID cmd = new XMLRPCCommandWorkAliveByUID(
				uri, config.getUser(), config.getHost());
		return workAliveByUid(cmd);
	}

	/**
	 * This checks the provided work accordingly to the server status
	 * 
	 * @param cmd
	 *            is the command to send
	 * @return an hashtable containing some parameters
	 */
	public XMLHashtable workAliveByUid(final XMLRPCCommandWorkAliveByUID cmd) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {

		try {
			sendCommand(cmd);
			return newXMLHashtable();
		} finally {
			close();
		}
	}

	/**
	 * This synchronizes with the server
	 * 
	 * @param p
	 *            is the workAlive parameter
	 * @return an hashtable containing some parameters
	 */
	public XMLHashtable workAlive(Hashtable p) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		try {
			final URI uri = newURI();
			final XMLRPCCommandWorkAlive cmd = new XMLRPCCommandWorkAlive(uri,
					config.getUser(), config.getHost(), p);
			return workAlive(cmd);
		} finally {
			close();
		}
	}

	/**
	 * This synchronizes with the server
	 * 
	 * @param cmd
	 *            is the command to send
	 * @return an hahtable containing some parameters
	 */
	public XMLHashtable workAlive(final XMLRPCCommandWorkAlive cmd) throws InvalidKeyException,
	AccessControlException, IOException, SAXException {

		try {
			sendCommand(cmd);
			return newXMLHashtable();
		} finally {
			close();
		}
	}

	/**
	 * This pings the server
	 */
	public void ping() throws InvalidKeyException, AccessControlException,
	IOException {

		try {
			final URI uri = newURI();
			final XMLRPCCommandPing cmd = new XMLRPCCommandPing(uri);
			ping(cmd);
		} catch (final URISyntaxException e2) {
		} finally {
			close();
		}
	}

	/**
	 * This pings the server
	 */
	public void ping(final XMLRPCCommandPing cmd) throws InvalidKeyException, AccessControlException,
	IOException {

		try {
			sendCommand(cmd);
			newXMLVector();
		} catch (final SAXException e) {
		} finally {
			close();
		}
	}

	/**
	 * This retrieves all traces from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getTraces() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetTraces cmd = new XMLRPCCommandGetTraces(uri,
				config.getUser());
		return getTraces(cmd);
	}

	/**
	 * This retrieves all traces from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getTraces(XMLRPCCommandGetTraces command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * Get all known traces.
	 * 
	 * @return an vector of traces UID
	 */
	public XMLVector getTraces(Date since, Date before) throws IOException,
	SAXException {
		throw new IOException("getTraces not implemented");
	}

	/**
	 * This retrieves all usergroups from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getUserGroups() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetUserGroups cmd = new XMLRPCCommandGetUserGroups(
				uri, config.getUser());
		return getUserGroups(cmd);
	}

	/**
	 * This retrieves all usergroups from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getUserGroups(XMLRPCCommandGetUserGroups command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		IOException ioe = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} catch (final IOException e) {
			logger.exception(e);
			ioe = e;
		} finally {
			close();
			if (ioe != null) {
				xmlv = null;
				throw ioe;
			}
		}
		return xmlv;
	}

	/**
	 * This calls getUser(login, true)
	 */
	public UserInterface getUser(String login) throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {
		return getUser(login, true);
	}

	/**
	 * This calls getUser(new XMLRPCCommandGetUserByLogin(login), bypass)
	 */
	public UserInterface getUser(String login, boolean bypass)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException, URISyntaxException {

		final URI uri = new URI(newURI().toString() + "/" + login);
		final XMLRPCCommandGetUserByLogin cmd = new XMLRPCCommandGetUserByLogin(
				uri);
		return getUser(cmd, bypass);
	}

	/**
	 * This calls getUser(command, true)
	 */
	public UserInterface getUser(XMLRPCCommandGetUserByLogin command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {
		return getUser(command, true);
	}

	/**
	 * This retrieves an user from server
	 * 
	 * @param command
	 *            is the command to send to server to retreive user
	 * @param bypass
	 *            if true user is downloaded from server even if already in
	 *            cache if false, user is only downloaded if not already in
	 *            cache
	 */
	public UserInterface getUser(XMLRPCCommandGetUserByLogin command,
			boolean bypass) throws InvalidKeyException, AccessControlException,
			IOException, SAXException {

		UserInterface user = null;

		if (!bypass) {
			user = cache.userByLogin(command.getLogin());
			if (user != null) {
				return user;
			}
		}

		try {
			sendCommand(command);
			user = newUserInterface();
		} finally {
			close();
		}
		return user;
	}

	/**
	 * This retrieves all users from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getUsers() throws InvalidKeyException,
	AccessControlException, IOException, SAXException,
	URISyntaxException {

		final URI uri = newURI();
		final XMLRPCCommandGetUsers cmd = new XMLRPCCommandGetUsers(uri,
				config.getUser());
		return getUsers(cmd);
	}

	/**
	 * This retrieves all users from server
	 * 
	 * @return a vector of UIDs
	 */
	public XMLVector getUsers(XMLRPCCommandGetUsers command)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException {

		XMLVector xmlv = null;
		try {
			sendCommand(command);
			xmlv = newXMLVector();
		} finally {
			close();
		}
		return xmlv;
	}

	/**
	 * This creates a Mobile Work filled with worker software. It is used to
	 * update worker as needed.
	 * 
	 * @return a filled mobileWork
	 */
	public WorkInterface getWorkerBin() throws IOException {
		throw new IOException("CommClient#getWorkerBin not implemented yet");
	}

	/*
	 * Tracer
	 */
	public void tactivityMonitor(long start, long end, byte[] file)
	throws IOException {
		throw new IOException("CommClient#tactivitymonotor not implemented yet");
	}

	/**
	 * Get trusted addresses
	 * 
	 * @return a string containing trused ip addresses separated by a white
	 *         space.
	 */
	public String getTrustedAddresses() throws IOException {
		throw new IOException(
		"CommClient#getTrustedAddresses not implemented yet");
	}

	/**
	 * Add a trusted address
	 * 
	 * @param ip
	 *            new trusted IP
	 */
	public void addTrustedAddress(String ip) throws IOException {
		throw new IOException(
		"CommClient#addtrustedaddress not implemented yet");
	}

	/**
	 * Remove a trusted address
	 * 
	 * @param ip
	 *            trusted IP to remove
	 */
	public void removeTrustedAddress(String ip) throws IOException {
		throw new IOException(
		"CommClient#removeTrustedAddress not implemented yet");
	}

	/**
	 * Set workers trace flag.
	 * 
	 * @param hosts
	 *            is a hashtable which contains host name as key and their
	 *            dedicated trace flag as value.
	 */
	public void traceWorkers(Hashtable hosts) throws IOException {
		throw new IOException("CommClient#traceWorkers not implemented yet");
	}

	/**
	 * This removes a set of jobs
	 * 
	 * @param jobs
	 *            is a Vector of URI
	 * @exception IOException
	 *                on connection error
	 */
	public void removeWorks(final Collection<URI> jobs) throws InvalidKeyException,
	AccessControlException, IOException {

		for (final Iterator<URI> li = jobs.iterator(); li.hasNext(); ) {
			remove(li.next());
		}
	}

	/**
	 * This retrieves job status
	 * 
	 * @param uid
	 *            is the UID of the job to retreive status for
	 * @return XWStatus.ERROR on error, job status otherwise
	 * @exception IOException
	 *                on connection error
	 */
	public StatusEnum jobStatus(UID uid) throws InvalidKeyException,
	AccessControlException, IOException, ClassNotFoundException,
	SAXException, URISyntaxException {

		StatusEnum status = StatusEnum.ERROR;

		final WorkInterface work = (WorkInterface) get(uid);
		status = work.getStatus();
		return status;

	}

	/**
	 * This retrieves a group status
	 * 
	 * @param group
	 *            describes the group
	 * @return XWStatus.ERROR on error, XWStatus.COMPLETED if all this group
	 *         jobs are completed, XWStatus.WAITING otherwise
	 * @exception IOException
	 *                on connection error
	 * @exception IOException
	 *                if the group UID is not set
	 */
	public StatusEnum groupStatus(GroupInterface group)
	throws InvalidKeyException, AccessControlException, IOException,
	ClassNotFoundException, SAXException, URISyntaxException {

		final XMLVector jobIDs = getGroupWorks(group.getUID());

		if (jobIDs == null) {
			return StatusEnum.ERROR;
		}

		final Vector<XMLValue> v = jobIDs.getXmlValues();
		try {
			for (final Enumeration<XMLValue> e = v.elements(); e
			.hasMoreElements();) {
				final UID uid = (UID) e.nextElement().getValue();
				final WorkInterface work = (WorkInterface) get(uid);
				if (work.getStatus() != StatusEnum.COMPLETED) {
					return StatusEnum.WAITING;
				}
			}
		} finally {
			v.clear();
		}
		return StatusEnum.COMPLETED;
	}

	/**
	 * This retrieves a session status
	 * 
	 * @param session
	 *            describes the session
	 * @return XWStatus.ERROR on error, XWStatus.COMPLETED if all results are
	 *         completed, XWStatus.WAITING if at least one is not completed
	 * @exception IOException
	 *                on connection error
	 * @exception IOException
	 *                if the session UID is not set
	 */
	public StatusEnum sessionStatus(SessionInterface session)
	throws InvalidKeyException, AccessControlException, IOException,
	ClassNotFoundException, SAXException, URISyntaxException {

		final XMLVector jobIDs = getSessionWorks(session.getUID());

		if (jobIDs == null) {
			return StatusEnum.ERROR;
		}

		final Vector<XMLValue> v = jobIDs.getXmlValues();
		try {
			for (final Enumeration<XMLValue> e = v.elements(); e
			.hasMoreElements();) {
				final UID uid = (UID) e.nextElement().getValue();
				final WorkInterface work = (WorkInterface) get(uid);
				if (work.getStatus() != StatusEnum.COMPLETED) {
					return StatusEnum.WAITING;
				}
			}
		} finally {
			v.clear();
		}
		return StatusEnum.COMPLETED;
	}

	/**
	 * This retrieves group results
	 * 
	 * @return a Vector of MobileResult or null on error
	 * @exception IOException
	 *                on connection error
	 */
	public XMLVector getResults() throws IOException {

		throw new IOException("CommClient::getResult() not implemented");
	}

	/**
	 * This retrieves group results
	 * 
	 * @param uids
	 *            is a Vector of UID
	 * @return a Vector of MobileResult or null on error
	 * @exception IOException
	 *                on connection error
	 */
	public XMLVector getResults(XMLVector uids) throws IOException {

		throw new IOException("CommClient::getResult() not implemented");
	}

	/**
	 * This retrieves group results
	 * 
	 * @param group
	 *            describes the group to retreive results for
	 * @return a Vector of MobileResult or null on error
	 * @exception IOException
	 *                on connection error
	 * @exception IOException
	 *                if the group UID is not set
	 */
	public XMLVector getGroupResults(GroupInterface group)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException, URISyntaxException {

		final XMLVector uids = getGroupWorks(group.getUID());
		return getResults(uids);
	}

	/**
	 * This retrieves session results
	 * 
	 * @param session
	 *            describes the session to retreive results for
	 * @return a Vector of MobileResult or null on error
	 * @exception IOException
	 *                on connection error
	 * @exception IOException
	 *                if the session UID is not set
	 */
	public XMLVector getSessionResults(SessionInterface session)
	throws InvalidKeyException, AccessControlException, IOException,
	SAXException, URISyntaxException {

		final XMLVector uids = getSessionWorks(session.getUID());
		return getResults(uids);
	}

	/**
	 * This method waits for works to complete, with a time out<br />
	 * If time out is reached, this returns the works completed within the given
	 * time
	 * 
	 * @param works
	 *            a Vector of UID
	 * @param deltaT
	 *            is the time out to wait, in milliseconds
	 * @exception IOException
	 *                on connection error
	 */
	public void getCompletedWorks(Collection<UID> works, long deltaT)
	throws InvalidKeyException, AccessControlException,
	ClassNotFoundException, SAXException, URISyntaxException, IOException {

		final long t0 = System.currentTimeMillis();
		int completeds = 0;

		while ((completeds < works.size())
				&& (deltaT > (System.currentTimeMillis() - t0))) {

			for (final Iterator<UID> iterator = works.iterator(); iterator.hasNext(); ) {
				try {
					waitForCompletedWork(iterator.next(), deltaT);
					completeds++;
				} catch (final InterruptedException e) {
				}
			}
		}
	}

	/**
	 * This method waits (for ever) until all works are completed<br />
	 * Keep in mind that this may never return
	 * 
	 * @param works
	 *            a Vector of UID
	 * @exception IOException
	 *                on connection error
	 */
	public void waitForCompletedWorks(Collection<UID> works)
	throws InvalidKeyException, AccessControlException,
	ClassNotFoundException, SAXException, URISyntaxException, IOException {
		try {
			waitForCompletedWorks(works, -1);
		} catch (final InterruptedException e) {
		}
	}

	/**
	 * This method waits until all works are completed, with a time out<br />
	 * If time out is reached an InterruptedException is thrown
	 * 
	 * @param works
	 *            a Vector of UID
	 * @param deltaT
	 *            is the time out to wait, in milliseconds
	 * @exception IOException
	 *                on connection error
	 * @exception InterruptedException
	 *                if time out reached
	 */
	public void waitForCompletedWorks(Collection<UID> works, long deltaT)
	throws InvalidKeyException, AccessControlException,
	InterruptedException, ClassNotFoundException, SAXException, IOException,
	URISyntaxException {

		long t0 = System.currentTimeMillis();
		int completeds = 0;

		while (completeds < works.size()) {

			if (deltaT < (System.currentTimeMillis() - t0)) {
				throw new InterruptedException("waitForCompletedWorks reached "
						+ deltaT);
			}

			for (final Iterator<UID> iterator = works.iterator(); iterator.hasNext(); ) {
				final UID uid = iterator.next();

				logger.debug("Waiting completion for " + uid);

				waitForCompletedWork(uid, deltaT);
				deltaT -= (System.currentTimeMillis() - t0);
				t0 = System.currentTimeMillis();
				completeds++;
			}
		}
	}

	/**
	 * This method waits until the work is completed<br />
	 * Keep in mind that this may never return
	 * 
	 * @param uid
	 *            is the UID of the expected work
	 * @exception IOException
	 *                on connection error
	 * @throws InterruptedException 
	 * @return the work found
	 */
	public WorkInterface waitForCompletedWork(final UID uid) throws InvalidKeyException,
	AccessControlException, ClassNotFoundException, SAXException, IOException, 
	URISyntaxException, InterruptedException {
		return waitForCompletedWork(uid, -1);
	}

	/**
	 * This method waits until the work is completed, with a time out
	 * 
	 * @param uid
	 *            is the UID of the expected work
	 * @param deltaT
	 *            is the time out in milliseconds
	 * @exception IOException
	 *                on connection error
	 * @exception InterruptedException
	 *                if time out reached
	 * @return the work found
	 */
	public WorkInterface waitForCompletedWork(final UID uid, final long deltaT)
	throws InvalidKeyException, AccessControlException,
	InterruptedException, ClassNotFoundException, SAXException,IOException,
	URISyntaxException {
		return waitForWork(StatusEnum.COMPLETED, uid, deltaT);
	}

	/**
	 * This waits until a work has the given status within the given time
	 * 
	 * @param status
	 *            is the status to wait for
	 * @param uid
	 *            is the uid of the expected work
	 * @param deltaT
	 *            is the time out in seconds
	 * @exception IOException
	 *                on connection error
	 * @exception InterruptedException
	 *                on time out error
	 * @return the work found
	 */
	public WorkInterface waitForWork(final StatusEnum status, final UID uid, final long deltaT)
	throws InvalidKeyException, AccessControlException,
	InterruptedException, ClassNotFoundException, IOException, SAXException,
	URISyntaxException {

		final long t0 = System.currentTimeMillis();

		while (true) {

			if ((deltaT > 0 ) && (deltaT < (System.currentTimeMillis() - t0))) {
				throw new InterruptedException("waitForWork " + uid + ","
						+ status + " reached " + deltaT);
			}
			final WorkInterface work = (WorkInterface) get(uid, true);
			if (work.getStatus() == status) {
				return work;
			}
			getLogger().debug("Sleeping " + 
					Math.max(100, Integer.parseInt(config
							.getProperty(XWPropertyDefs.TIMEOUT))));
			Thread.sleep(Math.max(100, Integer.parseInt(config
					.getProperty(XWPropertyDefs.TIMEOUT))));
		}
	}
}
